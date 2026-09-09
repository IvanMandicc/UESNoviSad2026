import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Event } from '../models/event.models';

/** [K4] / [M1] Rukovanje događajima. */
@Injectable({ providedIn: 'root' })
export class EventsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /** Događaji na mestu; podrazumevano samo predstojeći [K3]. */
  byLocation(locationId: number, all = false): Observable<Event[]> {
    const params = new HttpParams().set('all', all);
    return this.http.get<Event[]>(`${this.apiUrl}/locations/${locationId}/events`, { params });
  }

  get(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.apiUrl}/events/${id}`);
  }

  /** Slika se šalje kao multipart, pa ide FormData. */
  create(locationId: number, form: FormData): Observable<Event> {
    return this.http.post<Event>(`${this.apiUrl}/locations/${locationId}/events`, form);
  }

  update(id: number, form: FormData): Observable<Event> {
    return this.http.put<Event>(`${this.apiUrl}/events/${id}`, form);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/${id}`);
  }

  /** Da li prijavljeni korisnik sme da rukuje događajima na datom mestu. */
  canManage(locationId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/locations/${locationId}/events/permissions`);
  }

  /** <img> tag ne prolazi kroz interceptor, pa mu treba puna adresa. */
  imageUrl(event: Event): string {
    const origin = this.apiUrl.replace(/\/api\/?$/, '');
    return `${origin}${event.imageUrl}`;
  }
}
