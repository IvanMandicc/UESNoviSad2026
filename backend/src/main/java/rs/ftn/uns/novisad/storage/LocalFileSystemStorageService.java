package rs.ftn.uns.novisad.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.exception.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Skladistenje fajlova na lokalnom fajl sistemu.
 * Aktivno kada je app.storage.type = local (podrazumevano).
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileSystemStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileSystemStorageService.class);

    // PDF je dozvoljen zbog [UES] opisa mesta u slobodnoj formi.
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

    @Value("${app.storage.location}")
    private String storageLocation;

    private Path root;

    @PostConstruct
    void init() {
        this.root = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("Skladiste fajlova inicijalizovano: {}", root);
        } catch (IOException ex) {
            throw new IllegalStateException("Nije moguce kreirati folder za skladiste: " + root, ex);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Fajl je prazan.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Dozvoljene su samo slike (JPG, PNG, WEBP, GIF) i PDF dokumenti.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest("Nedozvoljena ekstenzija fajla: " + extension);
        }

        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Neuspesno cuvanje fajla {}", key, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Neuspesno cuvanje fajla.");
        }

        log.info("Sacuvan fajl [{}], velicina {} B", key, file.getSize());
        return key;
    }

    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw ApiException.notFound("Fajl nije pronadjen.");
        }
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException ex) {
            throw ApiException.notFound("Fajl nije pronadjen.");
        }
    }

    @Override
    public String contentType(String key) {
        try {
            return Files.probeContentType(resolve(key));
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            boolean deleted = Files.deleteIfExists(resolve(key));
            if (deleted) {
                log.info("Obrisan fajl [{}]", key);
            }
        } catch (IOException ex) {
            // Brisanje fajla ne sme da obori poslovnu operaciju.
            log.warn("Neuspesno brisanje fajla [{}]: {}", key, ex.getMessage());
        }
    }

    /** Sprecava izlazak iz root foldera (path traversal preko "..") . */
    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw ApiException.badRequest("Nedozvoljena putanja fajla.");
        }
        return resolved;
    }

    private static String extensionOf(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }
}
