package rs.ftn.uns.novisad.search;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rs.ftn.uns.novisad.dto.LocationSearchDto;
import rs.ftn.uns.novisad.dto.LocationSearchResultDto;

import java.util.ArrayList;
import java.util.List;

/**
 * [S1] Pretraga mesta u Elasticsearch-u.
 * <p>
 * Podrzana su pojedinacna polja (naziv, opis, sadrzaj PDF-a), opsezi (broj
 * utisaka i prosecne ocene po stavkama), kombinovanje polja AND/OR operatorom,
 * sortiranje po nazivu, dinamicki sazetak i "more like this" pretraga.
 */
@Service
public class LocationSearchService {

    private static final Logger log = LoggerFactory.getLogger(LocationSearchService.class);

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PDF = "pdfContent";

    /*
     * Granice za "more like this", odredjene empirijski nad postojecim skupom podataka.
     *
     * Podrazumevane vrednosti (min_term_freq 2, min_doc_freq 5) ne daju nijedan
     * rezultat jer je skup mali i termini se retko ponavljaju, pa su spustene na 1.
     *
     * minimum_should_match je podesen na "1" umesto podrazumevanih "30%": mesto sa
     * zakacenim PDF-om daje mnogo termina, pa procentualni prag nikada nije bio
     * ispunjen. Testirano je 30%, 25%, 20%, 10% i vrednosti 2 i 1 - tek "1" vraca
     * ocekivano slicna mesta. Kada baza naraste, prag se moze podici.
     */
    private static final int MLT_MIN_TERM_FREQ = 1;
    private static final int MLT_MAX_QUERY_TERMS = 25;
    private static final int MLT_MIN_DOC_FREQ = 1;
    private static final String MLT_MINIMUM_SHOULD_MATCH = "1";

    private static final int MAX_RESULTS = 100;

    private final ElasticsearchOperations operations;
    private final SearchQueryParser queryParser;

    public LocationSearchService(ElasticsearchOperations operations, SearchQueryParser queryParser) {
        this.operations = operations;
        this.queryParser = queryParser;
    }

    /** [S1] Pretraga po zadatim kriterijumima. */
    public List<LocationSearchResultDto> search(LocationSearchDto criteria) {
        List<Query> textQueries = new ArrayList<>();
        addTextQuery(textQueries, FIELD_NAME, criteria.name());
        addTextQuery(textQueries, FIELD_DESCRIPTION, criteria.description());
        addTextQuery(textQueries, FIELD_PDF, criteria.pdfContent());

        List<Query> rangeQueries = new ArrayList<>();
        addIntRange(rangeQueries, "reviewCount", criteria.minReviews(), criteria.maxReviews());
        addDoubleRange(rangeQueries, "ratingPerformance", criteria.minPerformance(), criteria.maxPerformance());
        addDoubleRange(rangeQueries, "ratingSoundAndLight", criteria.minSoundAndLight(), criteria.maxSoundAndLight());
        addDoubleRange(rangeQueries, "ratingSpace", criteria.minSpace(), criteria.maxSpace());
        addDoubleRange(rangeQueries, "ratingOverall", criteria.minOverall(), criteria.maxOverall());

        Query query = combine(textQueries, rangeQueries, criteria.useAndOperator());

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(MAX_RESULTS)
                .withHighlightQuery(new HighlightQuery(toSpringHighlight(), LocationDocument.class));

        // Sortiranje po nazivu koristi zasebno keyword polje; Text polje se ne moze sortirati.
        if (criteria.sortByName()) {
            SortOrder order = criteria.sortDescending() ? SortOrder.Desc : SortOrder.Asc;
            builder.withSort(s -> s.field(f -> f.field("nameSort").order(order)));
        }

        SearchHits<LocationDocument> hits = operations.search(builder.build(), LocationDocument.class);
        log.info("[S1] Pretraga vratila {} rezultata (operator={})",
                hits.getTotalHits(), criteria.useAndOperator() ? "AND" : "OR");

        return hits.getSearchHits().stream().map(LocationSearchService::toResult).toList();
    }

    /**
     * [S1] "More like this" - slicna mesta na osnovu naziva, opisa i PDF sadrzaja.
     */
    public List<LocationSearchResultDto> moreLikeThis(Long locationId) {
        Query query = QueryBuilders.moreLikeThis(m -> m
                .fields(FIELD_NAME, FIELD_DESCRIPTION, FIELD_PDF)
                .like(l -> l.document(d -> d.index("locations").id(String.valueOf(locationId))))
                .minTermFreq(MLT_MIN_TERM_FREQ)
                .maxQueryTerms(MLT_MAX_QUERY_TERMS)
                .minDocFreq(MLT_MIN_DOC_FREQ)
                .minimumShouldMatch(MLT_MINIMUM_SHOULD_MATCH));

        // Polazno mesto se izostavlja iz rezultata.
        Query withoutSelf = QueryBuilders.bool(b -> b
                .must(query)
                .mustNot(QueryBuilders.ids(i -> i.values(String.valueOf(locationId)))));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(withoutSelf)
                .withMaxResults(MAX_RESULTS)
                .build();

        SearchHits<LocationDocument> hits = operations.search(nativeQuery, LocationDocument.class);
        log.info("[S1] More-like-this za mesto {} vratio {} rezultata", locationId, hits.getTotalHits());

        return hits.getSearchHits().stream().map(LocationSearchService::toResult).toList();
    }

    // --- gradnja upita ---

    private void addTextQuery(List<Query> target, String field, String value) {
        if (StringUtils.hasText(value)) {
            target.add(queryParser.build(field, value));
        }
    }

    private static void addIntRange(List<Query> target, String field, Integer min, Integer max) {
        if (min == null && max == null) {
            return;
        }
        target.add(QueryBuilders.range(r -> r.number(n -> {
            n.field(field);
            if (min != null) {
                n.gte(min.doubleValue());
            }
            if (max != null) {
                n.lte(max.doubleValue());
            }
            return n;
        })));
    }

    private static void addDoubleRange(List<Query> target, String field, Double min, Double max) {
        if (min == null && max == null) {
            return;
        }
        target.add(QueryBuilders.range(r -> r.number(n -> {
            n.field(field);
            if (min != null) {
                n.gte(min);
            }
            if (max != null) {
                n.lte(max);
            }
            return n;
        })));
    }

    /**
     * [S1] BooleanQuery: tekstualna polja se kombinuju izabranim operatorom,
     * dok opsezi uvek suzavaju rezultat (uvek AND).
     */
    private static Query combine(List<Query> textQueries, List<Query> rangeQueries, boolean useAnd) {
        if (textQueries.isEmpty() && rangeQueries.isEmpty()) {
            return QueryBuilders.matchAll(m -> m);
        }

        return QueryBuilders.bool(b -> {
            if (!textQueries.isEmpty()) {
                if (useAnd) {
                    textQueries.forEach(b::must);
                } else {
                    textQueries.forEach(b::should);
                    b.minimumShouldMatch("1");
                }
            }
            rangeQueries.forEach(b::filter);
            return b;
        });
    }

    /** Dinamicki sazetak nad tekstualnim poljima, ukljucujuci sadrzaj PDF-a. */
    private static Highlight toSpringHighlight() {
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("<mark>")
                .withPostTags("</mark>")
                .build();

        List<HighlightField> fields = List.of(
                new HighlightField(FIELD_NAME),
                new HighlightField(FIELD_DESCRIPTION, fragmentParameters(150, 2)),
                new HighlightField(FIELD_PDF, fragmentParameters(150, 3)));

        return new Highlight(parameters, fields);
    }

    private static HighlightFieldParameters fragmentParameters(int fragmentSize, int numberOfFragments) {
        return HighlightFieldParameters.builder()
                .withFragmentSize(fragmentSize)
                .withNumberOfFragments(numberOfFragments)
                .build();
    }

    // --- mapiranje rezultata ---

    private static LocationSearchResultDto toResult(SearchHit<LocationDocument> hit) {
        LocationDocument doc = hit.getContent();

        List<String> highlights = hit.getHighlightFields().values().stream()
                .flatMap(List::stream)
                .toList();

        Long id = Long.valueOf(doc.getId());

        return new LocationSearchResultDto(
                id,
                doc.getName(),
                // Namerno opis iz korisnickog interfejsa, ne tekst iz PDF-a.
                doc.getDescription(),
                doc.getAddress(),
                doc.getType(),
                doc.getReviewCount(),
                doc.getRatingAverage(),
                doc.getRatingPerformance(),
                doc.getRatingSoundAndLight(),
                doc.getRatingSpace(),
                doc.getRatingOverall(),
                "/api/locations/" + id + "/image",
                doc.getPdfKey() == null ? null : "/api/locations/" + id + "/pdf",
                hit.getScore() == 0 ? null : (double) hit.getScore(),
                highlights
        );
    }
}
