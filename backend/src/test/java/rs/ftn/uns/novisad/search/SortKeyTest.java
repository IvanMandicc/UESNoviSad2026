package rs.ftn.uns.novisad.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [S1] Kljuc za sortiranje po nazivu mora biti isti bez obzira na pismo
 * i velicinu slova - inace bi isto mesto zauzimalo razlicito mesto u
 * abecednom redosledu zavisno od toga kako je uneto.
 * Ne trazi pokrenut Elasticsearch.
 */
class SortKeyTest {

    @Test
    @DisplayName("S1: cirilica i latinica daju isti kljuc za sortiranje")
    void cyrillicAndLatinMatch() {
        assertThat(LocationIndexService.sortKey("Студио М"))
                .isEqualTo(LocationIndexService.sortKey("Studio M"));
    }

    @Test
    @DisplayName("S1: velicina slova ne utice na kljuc")
    void caseDoesNotMatter() {
        assertThat(LocationIndexService.sortKey("STUDIO M"))
                .isEqualTo(LocationIndexService.sortKey("studio m"));
    }

    @Test
    @DisplayName("S1: dijakritici se svode na osnovna slova")
    void diacriticsAreStripped() {
        assertThat(LocationIndexService.sortKey("Čačak")).isEqualTo("cacak");
        assertThat(LocationIndexService.sortKey("Šabac")).isEqualTo("sabac");
        assertThat(LocationIndexService.sortKey("Žitište")).isEqualTo("zitiste");
    }

    @Test
    @DisplayName("S1: cirilicni digrafi se ispravno razlazu")
    void digraphsAreTransliterated() {
        assertThat(LocationIndexService.sortKey("Његош")).isEqualTo("njegos");
        assertThat(LocationIndexService.sortKey("Љубав")).isEqualTo("ljubav");
        assertThat(LocationIndexService.sortKey("Џез")).isEqualTo("dzez");
        assertThat(LocationIndexService.sortKey("Ђердап")).isEqualTo("djerdap");
    }

    @Test
    @DisplayName("S1: null i prazan naziv ne rusi izracunavanje")
    void handlesNull() {
        assertThat(LocationIndexService.sortKey(null)).isNull();
        assertThat(LocationIndexService.sortKey("   ")).isEmpty();
    }
}
