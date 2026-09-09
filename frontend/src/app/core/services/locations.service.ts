import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Location, LocationAttributes } from '../models/location.models';

/** [K3] Rukovanje mestima. */
@Injectable({ providedIn: 'root' })
export class LocationsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/locations`;

  list(): Observable<Location[]> {
    return this.http.get<Location[]>(this.baseUrl);
  }

  get(id: number): Observable<Location> {
    return this.http.get<Location>(`${this.baseUrl}/${id}`);
  }

  /** Slika se šalje kao multipart, pa ide FormData umesto JSON-a. */
  create(form: FormData): Observable<Location> {
    return this.http.post<Location>(this.baseUrl, form);
  }

  update(id: number, form: FormData): Observable<Location> {
    return this.http.put<Location>(`${this.baseUrl}/${id}`, form);
  }

  /** [K3] Menadžer mesta ažurira adresu, tip i opis. */
  updateAttributes(id: number, attributes: LocationAttributes): Observable<Location> {
    return this.http.patch<Location>(`${this.baseUrl}/${id}/attributes`, attributes);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Backend vraća putanju oblika "/api/locations/1/image"; ovde je pretvaramo
   * u punu adresu jer <img> tag ne prolazi kroz Angular interceptor.
   */
  imageUrl(location: Location): string {
    const origin = environment.apiUrl.replace(/\/api\/?$/, '');
    return `${origin}${location.imageUrl}`;
  }
}
