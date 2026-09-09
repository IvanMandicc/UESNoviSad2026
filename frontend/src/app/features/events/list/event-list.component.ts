import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EVENT_TYPES, EVENT_TYPE_LABELS, Event, EventType } from '../../../core/models/event.models';
import { Location } from '../../../core/models/location.models';
import { readApiError } from '../../../core/services/api-error.util';
import { EventsService } from '../../../core/services/events.service';
import { LocationsService } from '../../../core/services/locations.service';

/**
 * [K6] Stranica za događaje.
 * <p>
 * Podrazumevano prikazuje današnje događaje sa svih mesta. Moguće je pretražiti
 * po nazivu i adresi, filtrirati po tipu, mestu i ceni, i odabrati proizvoljan
 * datum u prošlosti ili budućnosti.
 */
@Component({
  selector: 'app-event-list',
  imports: [RouterLink, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './event-list.component.html',
  styleUrl: './event-list.component.scss'
})
export class EventListComponent {
  private readonly eventsService = inject(EventsService);
  private readonly locationsService = inject(LocationsService);

  readonly types = EVENT_TYPES;
  readonly typeLabels = EVENT_TYPE_LABELS;

  readonly events = signal<Event[]>([]);
  readonly locations = signal<Location[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  query = '';
  selectedType: EventType | null = null;
  selectedLocationId: number | null = null;
  selectedDate = '';
  priceFilter: 'ALL' | 'FREE' | 'PAID' = 'ALL';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  allDates = false;

  constructor() {
    this.load();
    this.locationsService.list().subscribe({
      next: (locations) => this.locations.set(locations),
      error: () => this.locations.set([])
    });
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.eventsService
      .search({
        query: this.query,
        type: this.selectedType,
        locationId: this.selectedLocationId,
        date: this.selectedDate || null,
        freeEntry: this.priceFilter === 'ALL' ? null : this.priceFilter === 'FREE',
        minPrice: this.priceFilter === 'PAID' ? this.minPrice : null,
        maxPrice: this.priceFilter === 'PAID' ? this.maxPrice : null,
        allDates: this.allDates || !!this.selectedDate ? this.allDates : false
      })
      .subscribe({
        next: (events) => {
          this.events.set(events);
          this.loading.set(false);
        },
        error: (error) => {
          this.errorMessage.set(readApiError(error));
          this.loading.set(false);
        }
      });
  }

  /** Prikazuje događaje sa svih datuma umesto samo današnjih. */
  toggleAllDates(): void {
    this.allDates = !this.allDates;
    if (this.allDates) {
      this.selectedDate = '';
    }
    this.load();
  }

  resetFilters(): void {
    this.query = '';
    this.selectedType = null;
    this.selectedLocationId = null;
    this.selectedDate = '';
    this.priceFilter = 'ALL';
    this.minPrice = null;
    this.maxPrice = null;
    this.allDates = false;
    this.load();
  }

  imageUrl(event: Event): string {
    return this.eventsService.imageUrl(event);
  }

  typeLabel(type: EventType): string {
    return this.typeLabels[type] ?? type;
  }

  /** Naslov liste zavisi od toga da li je izabran datum. */
  get heading(): string {
    if (this.selectedDate) {
      return 'Događaji na izabrani datum';
    }
    return this.allDates ? 'Svi događaji' : 'Današnji događaji';
  }
}
