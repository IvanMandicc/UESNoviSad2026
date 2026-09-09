export type Role = 'USER' | 'MANAGER' | 'ADMIN';

export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  city: string | null;
  phone: string | null;
  role: Role;
  /** [K10] Adresa slike profila; null kada korisnik nema sliku. */
  imageUrl: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

/** [K1] Telo zahteva za registraciju. */
export interface RegistrationRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  city?: string | null;
  phone?: string | null;
}

/** [A1] Zahtev za registraciju kako ga vidi administrator. */
export interface AccountRequest {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  city: string | null;
  phone: string | null;
  status: RequestStatus;
  createdAt: string;
  processedAt: string | null;
  rejectionReason: string | null;
}

/** Jedinstven oblik greske koji vraca backend. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>;
}
