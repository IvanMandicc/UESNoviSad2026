import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AccountRequest, User } from '../models/auth.models';

/** [A1] Administratorska obrada zahteva za registraciju. */
@Injectable({ providedIn: 'root' })
export class AccountRequestsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/registration-requests`;

  list(all = false): Observable<AccountRequest[]> {
    const params = new HttpParams().set('all', all);
    return this.http.get<AccountRequest[]>(this.baseUrl, { params });
  }

  approve(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/approve`, {});
  }

  reject(id: number, reason: string | null): Observable<AccountRequest> {
    return this.http.post<AccountRequest>(`${this.baseUrl}/${id}/reject`, { reason });
  }
}
