package rs.ftn.uns.novisad.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.Map;

/**
 * [UES] Mesto kako je indeksirano u Elasticsearch-u.
 * <p>
 * Tekstualna polja koriste analizator {@code serbian_custom} (vidi
 * {@code elasticsearch/location-settings.json}), koji upit i sadrzaj svodi na
 * mala slova i na latinicu, pa pretraga radi nezavisno od velicine slova
 * i od pisma. Uz svako tekstualno polje ide i podpolje {@code keyword}, koje
 * sluzi za sortiranje po nazivu.
 */
@Document(indexName = "locations")
@Setting(settingPath = "elasticsearch/location-settings.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDocument {

    /** Isti id kao u relacionoj bazi, da se zapisi lako povezuju. */
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "serbian_custom", searchAnalyzer = "serbian_custom")
    private String name;

    /**
     * Naziv u obliku pogodnom za sortiranje. Ne moze se sortirati po Text polju,
     * pa se cuva zasebno kao Keyword - malim slovima i na latinici.
     */
    @Field(type = FieldType.Keyword)
    private String nameSort;

    /** Opis koji se unosi kroz korisnicki interfejs. */
    @Field(type = FieldType.Text, analyzer = "serbian_custom", searchAnalyzer = "serbian_custom")
    private String description;

    /** Tekst izvucen iz zakacenog PDF dokumenta. */
    @Field(type = FieldType.Text, analyzer = "serbian_custom", searchAnalyzer = "serbian_custom")
    private String pdfContent;

    @Field(type = FieldType.Text, analyzer = "serbian_custom", searchAnalyzer = "serbian_custom")
    private String address;

    @Field(type = FieldType.Keyword)
    private String type;

    /** [S1] Broj utisaka - pretraga po opsegu (od - do). */
    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    /** [S1] Prosecna ocena po stavkama - pretraga po opsegu za svaku stavku. */
    @Field(type = FieldType.Double)
    private Double ratingPerformance;

    @Field(type = FieldType.Double)
    private Double ratingSoundAndLight;

    @Field(type = FieldType.Double)
    private Double ratingSpace;

    @Field(type = FieldType.Double)
    private Double ratingOverall;

    /** Ukupan prosek svih ocena. */
    @Field(type = FieldType.Double)
    private Double ratingAverage;

    /** Ime PDF fajla u skladistu, da bi se mogao preuzeti iz rezultata pretrage. */
    @Field(type = FieldType.Keyword, index = false)
    private String pdfKey;

    @Field(type = FieldType.Keyword, index = false)
    private String imageKey;

    /** Postavlja prosecne ocene po stavkama iz mape koju vraca ReviewService. */
    public void applyCategoryRatings(Map<String, Double> byCategory) {
        this.ratingPerformance = byCategory.get("PERFORMANCE");
        this.ratingSoundAndLight = byCategory.get("SOUND_AND_LIGHT");
        this.ratingSpace = byCategory.get("SPACE");
        this.ratingOverall = byCategory.get("OVERALL");
    }
}
