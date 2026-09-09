import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { User } from '../models/auth.models';
import { ChangePassword, Profile, UpdateProfile } from '../models/profile.models';

/** [K9] Promena lozinke i [K10] profil korisnika. */
@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  /** [K10] Podaci, utisci i mesta kojima korisnik upravlja. */
  me(): Observable<Profile> {
    return this.http.get<Profile>(`${this.baseUrl}/me`);
  }

  updateProfile(data: UpdateProfile): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/me`, data);
  }

  updateImage(image: File): Observable<User> {
    const form = new FormData();
    form.append('image', image);
    return this.http.post<User>(`${this.baseUrl}/me/image`, form);
  }

  /** [K9] Promena lozinke. */
  changePassword(data: ChangePassword): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/me/password`, data);
  }

  /** <img> tag ne prolazi kroz interceptor, pa mu treba puna adresa. */
  imageUrl(user: User): string | null {
    if (!user.imageUrl) {
      return null;
    }
    const origin = environment.apiUrl.replace(/\/api\/?$/, '');
    return `${origin}${user.imageUrl}`;
  }
}
