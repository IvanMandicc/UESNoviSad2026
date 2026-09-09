package rs.ftn.uns.novisad.search;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.exception.ApiException;

import java.io.IOException;

/**
 * [UES] Izvlacenje teksta iz PDF dokumenta, da bi se indeksirao u Elasticsearch
 * kao Text polje i pretrazivao punim tekstom.
 */
@Component
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    /** Gornja granica duzine teksta koji se indeksira, da dokument ne naraste previse. */
    private static final int MAX_LENGTH = 1_000_000;

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw ApiException.badRequest("Zasticen PDF dokument se ne moze procitati.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.isBlank()) {
                log.warn("PDF [{}] ne sadrzi tekst koji se moze izvuci.", file.getOriginalFilename());
                return null;
            }

            String normalized = text.replaceAll("\s+", " ").trim();
            if (normalized.length() > MAX_LENGTH) {
                normalized = normalized.substring(0, MAX_LENGTH);
            }

            log.info("Iz PDF-a [{}] izvuceno {} karaktera teksta.",
                    file.getOriginalFilename(), normalized.length());
            return normalized;

        } catch (IOException ex) {
            log.error("Neuspesno citanje PDF-a [{}]: {}", file.getOriginalFilename(), ex.getMessage());
            throw ApiException.badRequest("PDF dokument nije moguce procitati.");
        }
    }
}
