import { HttpErrorResponse } from '@angular/common/http';

import { ApiError } from '../models/auth.models';

/** Izvlaci citljivu poruku iz odgovora backend-a. */
export function readApiError(error: unknown, fallback = 'Doslo je do greske. Pokusajte ponovo.'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (error.status === 0) {
    return 'Server nije dostupan. Proverite da li je backend pokrenut.';
  }

  const body = error.error as ApiError | null;
  if (body?.fieldErrors) {
    const messages = Object.values(body.fieldErrors);
    if (messages.length > 0) {
      return messages.join(' ');
    }
  }
  return body?.message ?? fallback;
}
