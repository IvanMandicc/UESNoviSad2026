import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { EVENT_TYPE_LABELS, EVENT_TYPES, Event, EventType } from '../../../core/models/event.models';
import { readApiError } from '../../../core/services/api-error.util';
import { EventsService } from '../../../core/services/events.service';
import { LocationsService } from '../../../core/services/locations.service';

/** [K4] / [M1] Dodavanje i izmena događaja — menadžer mesta (ili administrator). */
@Component({
  selector: 'app-event-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './event-form.component.html',
  styleUrl: './event-form.component.scss'
})
export class EventFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly eventsService = inject(EventsService);
  private readonly locationsService = inject(LocationsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly types = EVENT_TYPES;
  readonly typeLabels = EVENT_TYPE_LABELS;

  readonly submitting = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly currentImageUrl = signal<string | null>(null);
  readonly locationName = signal<string>('');
  readonly eventId = signal<number | null>(null);
  readonly locationId = signal<number | null>(null);
  readonly isEdit = signal(false);

  selectedFile: File | null = null;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    address: ['', [Validators.required, Validators.maxLength(300)]],
    type: ['KONCERT' as EventType, [Validators.required]],
    date: ['', [Validators.required]],
    regular: [false],
    freeEntry: [true],
    price: ['']
  });

  constructor() {
    const eventIdParam = this.route.snapshot.paramMap.get('id');
    const locationIdParam = this.route.snapshot.paramMap.get('locationId');

    if (eventIdParam) {
      this.eventId.set(Number(eventIdParam));
      this.isEdit.set(true);
      this.loadExisting(Number(eventIdParam));
    } else if (locationIdParam) {
      this.locationId.set(Number(locationIdParam));
      this.prefillFromLocation(Number(locationIdParam));
    }
  }

  /** Adresa događaja se podrazumevano preuzima sa mesta. */
  private prefillFromLocation(locationId: number): void {
    this.loading.set(true);
    this.locationsService.get(locationId).subscribe({
      next: (location) => {
        this.locationName.set(location.name);
        this.form.patchValue({ address: location.address });
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  private loadExisting(id: number): void {
    this.loading.set(true);
    this.eventsService.get(id).subscribe({
      next: (event: Event) => {
        this.locationId.set(event.locationId);
        this.locationName.set(event.locationName);
        this.form.patchValue({
          name: event.name,
          address: event.address,
          type: event.type,
          // <input type="datetime-local"> ocekuje "yyyy-MM-ddTHH:mm".
          date: event.date.substring(0, 16),
          regular: event.regular,
          freeEntry: event.freeEntry,
          price: event.price !== null ? String(event.price) : ''
        });
        this.currentImageUrl.set(this.eventsService.imageUrl(event));
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  onFileSelected(event: globalThis.Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();

    if (!this.isEdit() && !this.selectedFile) {
      this.errorMessage.set('Slika događaja je obavezna.');
      return;
    }
    if (!values.freeEntry && !values.price.trim()) {
      this.errorMessage.set('Unesite cenu ulaska ili označite da je događaj besplatan.');
      return;
    }

    const payload = new FormData();
    payload.append('name', values.name);
    payload.append('address', values.address);
    payload.append('type', values.type);
    // Backend ocekuje ISO LocalDateTime; input daje "yyyy-MM-ddTHH:mm".
    payload.append('date', `${values.date}:00`.substring(0, 19));
    payload.append('regular', String(values.regular));
    payload.append('freeEntry', String(values.freeEntry));
    if (!values.freeEntry) {
      payload.append('price', values.price.trim());
    }
    if (this.selectedFile) {
      payload.append('image', this.selectedFile);
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const id = this.eventId();
    const locationId = this.locationId();
    const request$ =
      this.isEdit() && id !== null
        ? this.eventsService.update(id, payload)
        : this.eventsService.create(locationId!, payload);

    request$.subscribe({
      next: (event) => {
        this.submitting.set(false);
        void this.router.navigate(['/dogadjaji', event.id]);
      },
      error: (error) => {
        this.submitting.set(false);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  hasError(control: string): boolean {
    const field = this.form.get(control);
    return !!field && field.invalid && (field.dirty || field.touched);
  }

  get isFree(): boolean {
    return this.form.controls.freeEntry.value;
  }
}
