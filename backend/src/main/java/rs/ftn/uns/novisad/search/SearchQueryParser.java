package rs.ftn.uns.novisad.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.springframework.stereotype.Component;

/**
 * [S1] Pretprocesiranje unosa iz polja forme u odgovarajuci tip upita.
 * <p>
 * Podrzani oblici, prema specifikaciji:
 * <ul>
 *   <li>{@code "tacna fraza"} pod dvostrukim navodnicima - PhraseQuery</li>
 *   <li>{@code Ivan M*} sa zvezdicom na kraju - PrefixQuery</li>
 *   <li>{@code ~rizika} sa tildom na pocetku - FuzzyQuery</li>
 *   <li>sve ostalo - obican match upit</li>
 * </ul>
 * Nezavisnost od velicine slova i pisma resava analizator {@code serbian_custom},
 * koji se primenjuje i na sadrzaj i na upit.
 */
@Component
public class SearchQueryParser {

    /**
     * Dozvoljena razdaljina za FuzzyQuery. Vrednost 2 empirijski hvata
     * uobicajene greske u kucanju (zamenjena, izostavljena ili viska slova),
     * a jos uvek ne vraca previse nevezanih rezultata.
     */
    private static final String FUZZINESS = "2";

    /** Tip upita prepoznat iz unosa - koristi se i za prikaz korisniku. */
    public enum QueryKind {
        PHRASE, PREFIX, FUZZY, MATCH
    }

    public record ParsedQuery(QueryKind kind, String value) {
    }

    /** Prepoznaje tip upita iz sirovog unosa. */
    public ParsedQuery parse(String raw) {
        String input = raw == null ? "" : raw.trim();

        if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
            return new ParsedQuery(QueryKind.PHRASE, input.substring(1, input.length() - 1).trim());
        }
        if (input.startsWith("~") && input.length() > 1) {
            return new ParsedQuery(QueryKind.FUZZY, input.substring(1).trim());
        }
        if (input.endsWith("*") && input.length() > 1) {
            return new ParsedQuery(QueryKind.PREFIX, input.substring(0, input.length() - 1).trim());
        }
        return new ParsedQuery(QueryKind.MATCH, input);
    }

    /** Gradi upit nad jednim poljem, prema prepoznatom tipu. */
    public Query build(String field, String raw) {
        ParsedQuery parsed = parse(raw);
        String value = parsed.value();

        return switch (parsed.kind()) {
            case PHRASE -> QueryBuilders.matchPhrase(m -> m.field(field).query(value));
            case PREFIX -> QueryBuilders.matchPhrasePrefix(m -> m.field(field).query(value));
            case FUZZY -> QueryBuilders.match(m -> m.field(field).query(value).fuzziness(FUZZINESS));
            case MATCH -> QueryBuilders.match(m -> m.field(field).query(value));
        };
    }
}
