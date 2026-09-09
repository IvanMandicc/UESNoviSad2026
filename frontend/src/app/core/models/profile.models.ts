import { User } from './auth.models';
import { Location } from './location.models';
import { Review } from './review.models';

/** [K10] Profil korisnika. */
export interface Profile {
  user: User;
  /** Spisak svih utisaka koje je korisnik ostavio. */
  reviews: Review[];
  /** Mesta na kojima je korisnik menadžer. */
  managedLocations: Location[];
}

/** [K10] Promena dodatnih podataka na profilu. */
export interface UpdateProfile {
  firstName: string;
  lastName: string;
  city: string | null;
  phone: string | null;
}

/** [K9] Promena lozinke. */
export interface ChangePassword {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
