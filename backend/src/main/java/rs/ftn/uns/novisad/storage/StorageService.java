package rs.ftn.uns.novisad.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Apstrakcija nad skladistem binarnih fajlova (slike mesta i dogadjaja).
 * Trenutna implementacija pise na lokalni fajl sistem; u UES delu se dodaje
 * MinIO implementacija bez izmena u ostatku koda.
 */
public interface StorageService {

    /**
     * Cuva fajl i vraca kljuc pod kojim se kasnije cita.
     *
     * @param folder logicka fascikla, npr. "locations"
     */
    String store(MultipartFile file, String folder);

    Resource load(String key);

    /** Vraca MIME tip sacuvanog fajla, ili null ako se ne moze odrediti. */
    String contentType(String key);

    void delete(String key);
}
