export type RateCategory = 'PERFORMANCE' | 'SOUND_AND_LIGHT' | 'SPACE' | 'OVERALL';

/** Imena polja forme koja odgovaraju stavkama ocenjivanja. */
export type RateControl = 'performance' | 'soundAndLight' | 'space' | 'overall';

/** [K5] Stavke po kojima se ocenjuje mesto, redosledom iz specifikacije. */
export const RATE_CATEGORIES: { key: RateCategory; control: RateControl; label: string }[] = [
  { key: 'PERFORMANCE', control: 'performance', label: 'Nastup' },
  { key: 'SOUND_AND_LIGHT', control: 'soundAndLight', label: 'Zvuk i svetlo' },
  { key: 'SPACE', control: 'space', label: 'Prostor' },
  { key: 'OVERALL', control: 'overall', label: 'Ukupan utisak' }
];

export const RATE_CATEGORY_LABELS: Record<RateCategory, string> = {
  PERFORMANCE: 'Nastup',
  SOUND_AND_LIGHT: 'Zvuk i svetlo',
  SPACE: 'Prostor',
  OVERALL: 'Ukupan utisak'
};

/** [K5] Utisak ostavljen na mesto. */
export interface Review {
  id: number;
  locationId: number;
  locationName: string;
  eventId: number;
  eventName: string;
  eventDate: string;
  authorId: number;
  authorName: string;
  /** Samo ocenjene stavke; neocenjene se ne pojavljuju. */
  rates: Partial<Record<RateCategory, number>>;
  averageRate: number | null;
  comment: string | null;
  /** Koliko se puta događaj održao u trenutku pisanja utiska. */
  timesHeldAtReview: number;
  hidden: boolean;
  createdAt: string;
}

/** [K5] Telo zahteva za ostavljanje utiska. */
export interface CreateReview {
  eventId: number;
  performance?: number | null;
  soundAndLight?: number | null;
  space?: number | null;
  overall?: number | null;
  comment?: string | null;
}
