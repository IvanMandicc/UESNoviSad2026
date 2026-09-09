import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LOCATION_TYPE_LABELS, Location, LocationType } from '../../../core/models/location.models';
import { readApiError } from '../../../core/services/api-error.util';
import { AuthService } from '../../../core/services/auth.service';
import { LocationsService } from '../../../core/services/locations.service';

/** [K3] Pregled svih mesta. */
@Component({
  selector: 'app-location-list',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './location-list.component.html',
  styleUrl: './location-list.component.scss'
})
export class LocationListComponent {
  private readonly locationsService = inject(LocationsService);
  private readonly authService = inject(AuthService);

  readonly locations = signal<Location[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isAdmin = this.authService.isAdmin;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.locationsService.list().subscribe({
      next: (locations) => {
        this.locations.set(locations);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  imageUrl(location: Location): string {
    return this.locationsService.imageUrl(location);
  }

  typeLabel(type: LocationType): string {
    return LOCATION_TYPE_LABELS[type] ?? type;
  }
}
