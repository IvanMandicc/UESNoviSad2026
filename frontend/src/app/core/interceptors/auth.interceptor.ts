import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Kaci "Authorization: Bearer <token>" na svaki zahtev i
 * odjavljuje korisnika kada backend vrati 401 (istekao/nevalidan token).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token;

  const authorized = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authorized).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLoginAttempt = req.url.endsWith('/auth/login');
      if (error.status === 401 && !isLoginAttempt && authService.isLoggedIn()) {
        authService.clearSession();
      }
      return throwError(() => error);
    })
  );
};
