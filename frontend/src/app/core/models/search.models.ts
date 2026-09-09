/**
 * [S1] Parametri pretrage mesta u Elasticsearch-u.
 * Sva polja su opciona; prazna se izostavljaju iz upita.
 */
export interface LocationSearchCriteria {
  name?: string | null;
  description?: string | null;
  pdfContent?: string | null;
  minReviews?: number | null;
  maxReviews?: number | null;
  minPerformance?: number | null;
  maxPerformance?: number | null;
  minSoundAndLight?: number | null;
  maxSoundAndLight?: number | null;
  minSpace?: number | null;
  maxSpace?: number | null;
  minOverall?: number | null;
  maxOverall?: number | null;
  /** Operator između zadatih tekstualnih polja. */
  operator?: 'AND' | 'OR';
  /** "name" za sortiranje po nazivu; prazno znači po relevantnosti. */
  sortBy?: string | null;
  sortDirection?: 'asc' | 'desc' | null;
}

/** [S1] Jedan rezultat pretrage. */
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
