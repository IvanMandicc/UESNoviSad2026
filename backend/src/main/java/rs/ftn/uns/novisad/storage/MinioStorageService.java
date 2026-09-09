package rs.ftn.uns.novisad.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.exception.ApiException;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * [UES] Skladistenje slika i dokumenata u MinIO bazi.
 * Aktivno kada je app.storage.type = minio.
 * <p>
 * Isti interfejs kao {@link LocalFileSystemStorageService}, pa se prelazak svodi
 * na promenu jedne postavke - ostatak aplikacije ostaje netaknut.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

    private final MinioClient client;

    @Value("${app.storage.minio.bucket}")
    private String bucket;

    public MinioStorageService(MinioClient client) {
        this.client = client;
    }

    @PostConstruct
    void init() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Kreiran MinIO bucket [{}]", bucket);
            } else {
                log.info("MinIO bucket [{}] vec postoji", bucket);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Nije moguce pripremiti MinIO bucket: " + bucket, ex);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Fajl je prazan.");
        }

        String contentType = String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Dozvoljene su samo slike (JPG, PNG, WEBP, GIF) i PDF dokumenti.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest("Nedozvoljena ekstenzija fajla: " + extension);
        }

        String key = folder + "/" + UUID.randomUUID() + "." + extension;

        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, file.getSize(), -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            log.error("Neuspesno cuvanje u MinIO [{}]", key, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Neuspesno cuvanje fajla.");
        }

        log.info("Sacuvan objekat u MinIO [{}], velicina {} B", key, file.getSize());
        return key;
    }

    @Override
    public Resource load(String key) {
        try {
            InputStream stream = client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
            return new InputStreamResource(stream);
        } catch (Exception ex) {
            log.warn("Objekat nije pronadjen u MinIO [{}]: {}", key, ex.getMessage());
            throw ApiException.notFound("Fajl nije pronadjen.");
        }
    }

    @Override
    public String contentType(String key) {
        try {
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
            return stat.contentType();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
            log.info("Obrisan objekat iz MinIO [{}]", key);
        } catch (Exception ex) {
            // Brisanje fajla ne sme da obori poslovnu operaciju.
            log.warn("Neuspesno brisanje iz MinIO [{}]: {}", key, ex.getMessage());
        }
    }

    private static String extensionOf(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }
}
