package rs.ftn.uns.novisad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.ftn.uns.novisad.search.SearchQueryParser;
import rs.ftn.uns.novisad.search.SearchQueryParser.QueryKind;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [S1] Prepoznavanje tipa upita iz unosa u polju forme.
 * Ne trazi pokrenut Elasticsearch.
 */
class SearchQueryParserTest {

    private final SearchQueryParser parser = new SearchQueryParser();

    @Test
    @DisplayName("S1: navodnici daju PhraseQuery, bez navodnika u vrednosti")
    void recognizesPhrase() {
        var parsed = parser.parse("\"koncertna dvorana\"");

        assertThat(parsed.kind()).isEqualTo(QueryKind.PHRASE);
        assertThat(parsed.value()).isEqualTo("koncertna dvorana");
    }

    @Test
    @DisplayName("S1: zvezdica na kraju daje PrefixQuery, bez zvezdice u vrednosti")
    void recognizesPrefix() {
        var parsed = parser.parse("Ivan M*");

        assertThat(parsed.kind()).isEqualTo(QueryKind.PREFIX);
        assertThat(parsed.value()).isEqualTo("Ivan M");
    }

    @Test
    @DisplayName("S1: tilda na pocetku daje FuzzyQuery, bez tilde u vrednosti")
    void recognizesFuzzy() {
        var parsed = parser.parse("~rizika");

        assertThat(parsed.kind()).isEqualTo(QueryKind.FUZZY);
        assertThat(parsed.value()).isEqualTo("rizika");
    }

    @Test
    @DisplayName("S1: obican unos daje match upit")
    void fallsBackToMatch() {
        var parsed = parser.parse("studio");

        assertThat(parsed.kind()).isEqualTo(QueryKind.MATCH);
        assertThat(parsed.value()).isEqualTo("studio");
    }

    @Test
    @DisplayName("S1: sam znak nije oznaka tipa upita")
    void singleCharacterIsNotMarker() {
        assertThat(parser.parse("*").kind()).isEqualTo(QueryKind.MATCH);
        assertThat(parser.parse("~").kind()).isEqualTo(QueryKind.MATCH);
        assertThat(parser.parse("\"").kind()).isEqualTo(QueryKind.MATCH);
    }

    @Test
    @DisplayName("S1: prazan i null unos ne rusi parser")
    void handlesEmptyInput() {
        assertThat(parser.parse(null).value()).isEmpty();
        assertThat(parser.parse("   ").value()).isEmpty();
    }

    @Test
    @DisplayName("S1: visak razmaka se uklanja")
    void trimsWhitespace() {
        assertThat(parser.parse("  \" tacna fraza \"  ").value()).isEqualTo("tacna fraza");
        assertThat(parser.parse("  ~ pojam  ").value()).isEqualTo("pojam");
    }
}
