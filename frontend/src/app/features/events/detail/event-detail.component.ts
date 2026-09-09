import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { EVENT_TYPE_LABELS, Event, EventType } from '../../../core/models/event.models';
import { readApiError } from '../../../core/services/api-error.util';
import { EventsService } from '../../../core/services/events.service';

/** [K4] Stranica događaja. Izmena i uklanjanje su dostupni menadžeru mesta. */
@Component({
  selector: 'app-event-detail',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.scss'
})
export class EventDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventsService = inject(EventsService);

  readonly event = signal<Event | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly canManage = signal(false);

  constructor() {
    this.load(Number(this.route.snapshot.paramMap.get('id')));
  }

  private load(id: number): void {
    this.loading.set(true);
    this.eventsService.get(id).subscribe({
      next: (event) => {
        this.event.set(event);
        this.loading.set(false);
        this.checkPermissions(event.locationId);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  /** Dugmad za izmenu se prikazuju samo menadžeru tog mesta (ili administratoru). */
  private checkPermissions(locationId: number): void {
    this.eventsService.canManage(locationId).subscribe({
      next: (allowed) => this.canManage.set(allowed),
      error: () => this.canManage.set(false)
    });
  }

  imageUrl(): string {
    const event = this.event();
    return event ? this.eventsService.imageUrl(event) : '';
  }

  typeLabel(type: EventType): string {
    return EVENT_TYPE_LABELS[type] ?? type;
  }

  remove(): void {
    const event = this.event();
    if (!event || !confirm(`Ukloniti događaj "${event.name}"?`)) {
      return;
    }

    this.eventsService.remove(event.id).subscribe({
      next: () => void this.router.navigate(['/mesta', event.locationId]),
      error: (error) => this.errorMessage.set(readApiError(error))
    });
  }
}
