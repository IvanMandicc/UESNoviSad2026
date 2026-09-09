export type LocationType =
  | 'KLUB'
  | 'KAFIC'
  | 'RESTORAN'
  | 'KONCERTNA_DVORANA'
  | 'POZORISTE'
  | 'BIOSKOP'
  | 'STADION'
  | 'KULTURNI_CENTAR'
  | 'GALERIJA'
  | 'OTVORENI_PROSTOR'
  | 'OSTALO';

/** Nazivi tipova mesta za prikaz u interfejsu. */
export const LOCATION_TYPE_LABELS: Record<LocationType, string> = {
  KLUB: 'Klub',
  KAFIC: 'Kafić',
  RESTORAN: 'Restoran',
  KONCERTNA_DVORANA: 'Koncertna dvorana',
  POZORISTE: 'Pozorište',
  BIOSKOP: 'Bioskop',
  STADION: 'Stadion',
  KULTURNI_CENTAR: 'Kulturni centar',
  GALERIJA: 'Galerija',
  OTVORENI_PROSTOR: 'Otvoreni prostor',
  OSTALO: 'Ostalo'
};

export const LOCATION_TYPES = Object.keys(LOCATION_TYPE_LABELS) as LocationType[];

/** [A2] Menadžer dodeljen mestu. */
export interface LocationManager {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  assignedAt: string;
}

/** [K3] Mesto. */
export interface Location {
  id: number;
  name: string;
  address: string;
  type: LocationType;
  description: string;
  imageUrl: string;
  /** [K5] Prosečna ocena — popunjava se kada bude implementiran K5. */
  averageRating: number | null;
  reviewCount: number;
  managers: LocationManager[];
  createdAt: string;
  updatedAt: string | null;
}

/** [K3] Atributi koje menadžer mesta sme da menja. */
export interface LocationAttributes {
  address: string;
  type: LocationType;
  description: string;
}
