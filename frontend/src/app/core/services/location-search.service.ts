import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { LocationSearchCriteria, LocationSearchResult } from '../models/search.models';

/** [S1] Pretraga mesta u Elasticsearch-u. */
@Injectable({ providedIn: 'root' })
export class LocationSearchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/search`;

  search(criteria: LocationSearchCriteria): Observable<LocationSearchResult[]> {
    return this.http.post<LocationSearchResult[]>(`${this.baseUrl}/locations`, criteria);
  }

  /** [S1] Slična mesta na osnovu naziva, opisa i sadržaja PDF-a. */
  similar(locationId: number): Observable<LocationSearchResult[]> {
    return this.http.get<LocationSearchResult[]>(`${this.baseUrl}/locations/${locationId}/similar`);
  }

  /** Administratorska alatka: ponovno indeksiranje svih mesta. */
  reindex(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/reindex`, {});
  }

  /** <img>/<a> ne prolaze kroz interceptor, pa im treba puna adresa. */
  absoluteUrl(path: string | null): string | null {
    if (!path) {
      return null;
    }
    const origin = environment.apiUrl.replace(/\/api\/?$/, '');
    return `${origin}${path}`;
  }
}
