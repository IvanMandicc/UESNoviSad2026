import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Event } from '../models/event.models';
import { CreateReview, Review } from '../models/review.models';

/** [K5] Ostavljanje i pregled utisaka. */
@Injectable({ providedIn: 'root' })
export class ReviewsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  byLocation(locationId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.apiUrl}/locations/${locationId}/reviews`);
  }

  /** Redovni događaji koji su se održali, a korisnik ih još nije ocenio. */
  reviewableEvents(locationId: number): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.apiUrl}/locations/${locationId}/reviewable-events`);
  }

  create(locationId: number, review: CreateReview): Observable<Review> {
    return this.http.post<Review>(`${this.apiUrl}/locations/${locationId}/reviews`, review);
  }

  get(id: number): Observable<Review> {
    return this.http.get<Review>(`${this.apiUrl}/reviews/${id}`);
  }
}
