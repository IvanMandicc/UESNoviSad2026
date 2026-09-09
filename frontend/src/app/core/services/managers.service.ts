import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { User } from '../models/auth.models';
import { LocationManager } from '../models/location.models';

/** [A2] Upravljanje menadžerima mesta — samo administrator. */
@Injectable({ providedIn: 'root' })
export class ManagersService {
  private readonly http = inject(HttpClient);

  list(locationId: number): Observable<LocationManager[]> {
    return this.http.get<LocationManager[]>(this.managersUrl(locationId));
  }

  assign(locationId: number, userId: number): Observable<LocationManager> {
    return this.http.post<LocationManager>(this.managersUrl(locationId), { userId });
  }

  remove(locationId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.managersUrl(locationId)}/${userId}`);
  }

  /** Korisnici koje administrator može da postavi za menadžera. */
  candidates(): Observable<User[]> {
    return this.http.get<User[]>(`${environment.apiUrl}/admin/users`);
  }

  private managersUrl(locationId: number): string {
    return `${environment.apiUrl}/admin/locations/${locationId}/managers`;
  }
}
