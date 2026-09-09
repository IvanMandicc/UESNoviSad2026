export type EventType =
  | 'KONCERT'
  | 'ZURKA'
  | 'FESTIVAL'
  | 'POZORISNA_PREDSTAVA'
  | 'PROJEKCIJA_FILMA'
  | 'IZLOZBA'
  | 'SPORTSKI_DOGADJAJ'
  | 'STAND_UP'
  | 'KARAOKE'
  | 'RADIONICA'
  | 'OSTALO';

/** Nazivi tipova događaja za prikaz u interfejsu. */
export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  KONCERT: 'Koncert',
  ZURKA: 'Žurka',
  FESTIVAL: 'Festival',
  POZORISNA_PREDSTAVA: 'Pozorišna predstava',
  PROJEKCIJA_FILMA: 'Projekcija filma',
  IZLOZBA: 'Izložba',
  SPORTSKI_DOGADJAJ: 'Sportski događaj',
  STAND_UP: 'Stand-up',
  KARAOKE: 'Karaoke',
  RADIONICA: 'Radionica',
  OSTALO: 'Ostalo'
};

export const EVENT_TYPES = Object.keys(EVENT_TYPE_LABELS) as EventType[];

/** [K4] Događaj koji se održava na nekom mestu. */
export interface Event {
  id: number;
  name: string;
  locationId: number;
  locationName: string;
  address: string;
  type: EventType;
  /** ISO datum i vreme, npr. "2026-10-05T21:00:00". */
  date: string;
  regular: boolean;
  freeEntry: boolean;
  price: number | null;
  imageUrl: string;
  hasTakenPlace: boolean;
  /** [K5] Koliko se puta događaj ukupno održao; popunjeno samo na detaljima. */
  timesHeld: number | null;
  createdAt: string;
  updatedAt: string | null;
}
