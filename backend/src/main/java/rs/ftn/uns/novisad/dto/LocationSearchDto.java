package rs.ftn.uns.novisad.dto;

/**
 * [S1] Parametri pretrage mesta u Elasticsearch-u.
 * <p>
 * Sva polja su opciona; prazna se izostavljaju iz upita. Tekstualna polja
 * prihvataju "fraza" pod navodnicima, prefiks* i ~fuzzy oblik.
 */
public record LocationSearchDto(
        String name,
        String description,
        String pdfContent,
        Integer minReviews,
        Integer maxReviews,
        Double minPerformance,
        Double maxPerformance,
        Double minSoundAndLight,
        Double maxSoundAndLight,
        Double minSpace,
        Double maxSpace,
        Double minOverall,
        Double maxOverall,
        /** "AND" ili "OR" - operator izmedju zadatih tekstualnih polja. */
        String operator,
        /** "name" za sortiranje po nazivu; prazno znaci po relevantnosti. */
        String sortBy,
        String sortDirection
) {
    public boolean useAndOperator() {
        return operator == null || operator.isBlank() || "AND".equalsIgnoreCase(operator);
    }

    public boolean sortByName() {
        return "name".equalsIgnoreCase(sortBy);
    }

    public boolean sortDescending() {
        return "desc".equalsIgnoreCase(sortDirection);
    }
}
