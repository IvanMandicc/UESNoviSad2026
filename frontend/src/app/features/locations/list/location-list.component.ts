import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  LOCATION_TYPES,
  LOCATION_TYPE_LABELS,
  Location,
  LocationType
} from '../../../core/models/location.models';
import { readApiError } from '../../../core/services/api-error.util';
import { AuthService } from '../../../core/services/auth.service';
import { LocationsService } from '../../../core/services/locations.service';

/** [K3] Pregled svih mesta. */
@Component({
  selector: 'app-location-list',
  imports: [RouterLink, DecimalPipe, FormsModule],
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

  /** [K6] Pretraga po nazivu ili adresi i filtriranje po tipu mesta. */
  readonly types = LOCATION_TYPES;
  readonly typeLabels = LOCATION_TYPE_LABELS;
  query = '';
  selectedType: LocationType | null = null;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.locationsService.list(this.query, this.selectedType).subscribe({
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

  /** [K6] Vraća listu na sva mesta. */
  resetFilters(): void {
    this.query = '';
    this.selectedType = null;
    this.load();
  }

  get hasFilters(): boolean {
    return this.query.trim().length > 0 || this.selectedType !== null;
  }

  imageUrl(location: Location): string {
    return this.locationsService.imageUrl(location);
  }

  /** [UES] Preuzimanje PDF opisa direktno iz prikaza svih mesta. */
  pdfUrl(location: Location): string | null {
    return location.pdfUrl ? this.locationsService.absoluteUrl(location.pdfUrl) : null;
  }

  typeLabel(type: LocationType): string {
    return LOCATION_TYPE_LABELS[type] ?? type;
  }
}
