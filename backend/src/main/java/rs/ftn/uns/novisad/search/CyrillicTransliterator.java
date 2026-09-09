package rs.ftn.uns.novisad.search;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [UES] Preslovljavanje cirilice u latinicu.
 * <p>
 * Isto preslikavanje koje radi {@code cyrillic_to_latin} char filter u
 * Elasticsearch analizatoru (vidi {@code elasticsearch/location-settings.json}).
 * Ovde je potrebno da bi kljuc za sortiranje po nazivu bio u istom obliku kao
 * indeksirani tekst.
 */
final class CyrillicTransliterator {

    /**
     * Redosled je bitan: dvoslovni znaci (Љ, Њ, Џ) moraju pre pojedinacnih slova,
     * inace bi se razlozili na pogresan nacin.
     */
    private static final Map<String, String> MAPPING = new LinkedHashMap<>();

    static {
        MAPPING.put("Љ", "Lj");
        MAPPING.put("љ", "lj");
        MAPPING.put("Њ", "Nj");
        MAPPING.put("њ", "nj");
        MAPPING.put("Џ", "Dz");
        MAPPING.put("џ", "dz");
        MAPPING.put("Ђ", "Dj");
        MAPPING.put("ђ", "dj");

        MAPPING.put("А", "A");
        MAPPING.put("а", "a");
        MAPPING.put("Б", "B");
        MAPPING.put("б", "b");
        MAPPING.put("В", "V");
        MAPPING.put("в", "v");
        MAPPING.put("Г", "G");
        MAPPING.put("г", "g");
        MAPPING.put("Д", "D");
        MAPPING.put("д", "d");
        MAPPING.put("Е", "E");
        MAPPING.put("е", "e");
        MAPPING.put("Ж", "Z");
        MAPPING.put("ж", "z");
        MAPPING.put("З", "Z");
        MAPPING.put("з", "z");
        MAPPING.put("И", "I");
        MAPPING.put("и", "i");
        MAPPING.put("Ј", "J");
        MAPPING.put("ј", "j");
        MAPPING.put("К", "K");
        MAPPING.put("к", "k");
        MAPPING.put("Л", "L");
        MAPPING.put("л", "l");
        MAPPING.put("М", "M");
        MAPPING.put("м", "m");
        MAPPING.put("Н", "N");
        MAPPING.put("н", "n");
        MAPPING.put("О", "O");
        MAPPING.put("о", "o");
        MAPPING.put("П", "P");
        MAPPING.put("п", "p");
        MAPPING.put("Р", "R");
        MAPPING.put("р", "r");
        MAPPING.put("С", "S");
        MAPPING.put("с", "s");
        MAPPING.put("Т", "T");
        MAPPING.put("т", "t");
        MAPPING.put("Ћ", "C");
        MAPPING.put("ћ", "c");
        MAPPING.put("У", "U");
        MAPPING.put("у", "u");
        MAPPING.put("Ф", "F");
        MAPPING.put("ф", "f");
        MAPPING.put("Х", "H");
        MAPPING.put("х", "h");
        MAPPING.put("Ц", "C");
        MAPPING.put("ц", "c");
        MAPPING.put("Ч", "C");
        MAPPING.put("ч", "c");
        MAPPING.put("Ш", "S");
        MAPPING.put("ш", "s");
    }

    private CyrillicTransliterator() {
    }

    static String toLatin(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String result = value;
        for (Map.Entry<String, String> entry : MAPPING.entrySet()) {
            if (result.indexOf(entry.getKey().charAt(0)) >= 0) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
