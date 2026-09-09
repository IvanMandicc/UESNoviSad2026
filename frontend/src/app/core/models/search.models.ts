/**
 * [S1] Parametri pretrage mesta u Elasticsearch-u: naziv, opis, sadržaj PDF-a
 * i opseg broja utisaka. Sva polja su opciona; prazna se izostavljaju iz upita.
 * <p>
 * Backend (LocationSearchDto) prima i polja za opseg ocene po kategorijama,
 * BooleanQuery operator i sortiranje — postoje i rade, samo se odavde ne šalju
 * jer nisu deo tražene funkcionalnosti.
 */
export interface LocationSearchCriteria {
  name?: string | null;
  description?: string | null;
  pdfContent?: string | null;
  minReviews?: number | null;
  maxReviews?: number | null;
}

/**
 * [S1] Jedan rezultat pretrage.
 * <p>
 * Backend uvek vraća score, highlights i ocene po kategorijama (deo su
 * odgovora nezavisno od upita), ali ih trenutni prikaz ne koristi jer su
 * vezani za funkcionalnosti van traženog obima (Highlighter, opseg ocena).
 */
export interface LocationSearchResult {
  id: number;
  name: string;
  /** Opis zadat iz interfejsa — namerno ne onaj iz PDF-a. */
  description: string;
  address: string;
  type: string;
  reviewCount: number | null;
  ratingAverage: number | null;
  ratingPerformance: number | null;
  ratingSoundAndLight: number | null;
  ratingSpace: number | null;
  ratingOverall: number | null;
  imageUrl: string;
  /** Adresa za preuzimanje PDF-a; null kada mesto nema dokument. */
  pdfUrl: string | null;
  score: number | null;
  /** Isečci teksta sa istaknutim pogotkom (<mark>). */
  highlights: string[];
}
