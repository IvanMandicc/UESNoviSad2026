import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AccountRequest, RegistrationRequest } from '../models/auth.models';

/** [K1] Slanje zahteva za registraciju. */
@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);

  submit(request: RegistrationRequest): Observable<AccountRequest> {
    return this.http.post<AccountRequest>(`${environment.apiUrl}/registration-requests`, request);
  }
}
