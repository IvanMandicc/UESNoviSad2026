package rs.ftn.uns.novisad.dto;

import java.util.List;

/**
 * [S1] Jedan rezultat pretrage.
 * <p>
 * Prikazuje se naziv i opis koji je zadat iz korisnickog interfejsa - ne onaj
 * iz PDF-a. Dinamicki sazetak ({@code highlights}) pokazuje gde se pojam nasao,
 * ukljucujuci i pogodak unutar PDF sadrzaja.
 */
public record LocationSearchResultDto(
        Long id,
        String name,
        String description,
        String address,
        String type,
        Integer reviewCount,
        Double ratingAverage,
        Double ratingPerformance,
        Double ratingSoundAndLight,
        Double ratingSpace,
        Double ratingOverall,
        String imageUrl,
        /** Adresa za preuzimanje PDF-a; null kada mesto nema zakacen dokument. */
        String pdfUrl,
        Double score,
        /** Isecci teksta sa istaknutim pogotkom, po polju. */
        List<String> highlights
) {
}
