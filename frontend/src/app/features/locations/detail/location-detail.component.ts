import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { User } from '../../../core/models/auth.models';
import { EVENT_TYPE_LABELS, Event, EventType } from '../../../core/models/event.models';
import { RATE_CATEGORIES, RateCategory, Review } from '../../../core/models/review.models';
import {
  LOCATION_TYPE_LABELS,
  LOCATION_TYPES,
  Location,
  LocationType
} from '../../../core/models/location.models';
import { readApiError } from '../../../core/services/api-error.util';
import { AuthService } from '../../../core/services/auth.service';
import { EventsService } from '../../../core/services/events.service';
import { ReviewsService } from '../../../core/services/reviews.service';
import { LocationsService } from '../../../core/services/locations.service';
import { ManagersService } from '../../../core/services/managers.service';

/**
 * [K3] Stranica mesta. Predstojeći događaji [K4] i prosečna ocena [K5]
 * su prikazani kao prazne sekcije dok ti zahtevi ne budu implementirani.
 * [A2] Administrator ovde dodaje i uklanja menadžere mesta.
 */
@Component({
  selector: 'app-location-detail',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './location-detail.component.html',
  styleUrl: './location-detail.component.scss'
})
export class LocationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly locationsService = inject(LocationsService);
  private readonly managersService = inject(ManagersService);
  private readonly eventsService = inject(EventsService);
  private readonly reviewsService = inject(ReviewsService);
  private readonly authService = inject(AuthService);

  readonly types = LOCATION_TYPES;
  readonly typeLabels = LOCATION_TYPE_LABELS;

  readonly location = signal<Location | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(null);
  readonly editingAttributes = signal(false);
  readonly savingAttributes = signal(false);

  /** [K4] Predstojeci dogadjaji na ovom mestu. */
  readonly events = signal<Event[]>([]);
  readonly eventsLoading = signal(false);
  readonly showPastEvents = signal(false);
  /** [K4] Da li prijavljeni korisnik sme da rukuje dogadjajima na ovom mestu. */
  readonly canManageEvents = signal(false);

  /** [K5] Utisci ostavljeni na ovo mesto. */
  readonly reviews = signal<Review[]>([]);
  readonly reviewsLoading = signal(false);
  readonly rateCategories = RATE_CATEGORIES;

  readonly candidates = signal<User[]>([]);
  readonly assigningUserId = signal<number | null>(null);
  selectedCandidateId: number | null = null;

  readonly isAdmin = this.authService.isAdmin;

  /** [K3] Menadžer sme da menja atribute samo mesta kojim upravlja. */
  readonly canEditAttributes = computed(() => {
    const current = this.authService.user();
    const loc = this.location();
    if (!current || !loc) {
      return false;
    }
    return current.role === 'ADMIN' || loc.managers.some((m) => m.userId === current.id);
  });

  readonly attributesForm = this.fb.nonNullable.group({
    address: ['', [Validators.required, Validators.maxLength(300)]],
    type: ['KLUB' as LocationType, [Validators.required]],
    description: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.locationsService.get(id).subscribe({
      next: (location) => {
        this.location.set(location);
        this.attributesForm.patchValue({
          address: location.address,
          type: location.type,
          description: location.description
        });
        this.loading.set(false);
        this.loadEvents(id);
        this.loadReviews(id);
        this.eventsService.canManage(id).subscribe({
          next: (allowed) => this.canManageEvents.set(allowed),
          error: () => this.canManageEvents.set(false)
        });
        if (this.isAdmin()) {
          this.loadCandidates();
        }
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  private loadCandidates(): void {
    this.managersService.candidates().subscribe({
      next: (users) => this.candidates.set(users),
      error: () => this.candidates.set([])
    });
  }

  /** Korisnici koji još nisu menadžeri ovog mesta. */
  readonly availableCandidates = computed(() => {
    const loc = this.location();
    if (!loc) {
      return [];
    }
    const assigned = new Set(loc.managers.map((m) => m.userId));
    return this.candidates().filter((user) => !assigned.has(user.id));
  });

  imageUrl(): string {
    const loc = this.location();
    return loc ? this.locationsService.imageUrl(loc) : '';
  }

  typeLabel(type: LocationType): string {
    return this.typeLabels[type] ?? type;
  }

  /** [UES] Puna adresa za preuzimanje PDF-a; <a> ne prolazi kroz interceptor. */
  pdfUrl(location: Location): string | null {
    return location.pdfUrl ? this.locationsService.absoluteUrl(location.pdfUrl) : null;
  }

  startEditingAttributes(): void {
    this.editingAttributes.set(true);
    this.clearMessages();
  }

  cancelEditingAttributes(): void {
    const loc = this.location();
    if (loc) {
      this.attributesForm.patchValue({
        address: loc.address,
        type: loc.type,
        description: loc.description
      });
    }
    this.editingAttributes.set(false);
  }

  saveAttributes(): void {
    const loc = this.location();
    if (!loc || this.attributesForm.invalid) {
      this.attributesForm.markAllAsTouched();
      return;
    }

    this.savingAttributes.set(true);
    this.clearMessages();

    this.locationsService.updateAttributes(loc.id, this.attributesForm.getRawValue()).subscribe({
      next: () => {
        this.savingAttributes.set(false);
        this.editingAttributes.set(false);
        this.infoMessage.set('Atributi mesta su ažurirani.');
        this.load(loc.id);
      },
      error: (error) => {
        this.savingAttributes.set(false);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  /** [A2] Dodeljivanje menadžera mestu. */
  assignManager(): void {
    const loc = this.location();
    if (!loc || this.selectedCandidateId === null) {
      return;
    }

    this.assigningUserId.set(this.selectedCandidateId);
    this.clearMessages();

    this.managersService.assign(loc.id, this.selectedCandidateId).subscribe({
      next: (manager) => {
        this.assigningUserId.set(null);
        this.selectedCandidateId = null;
        this.infoMessage.set(`${manager.firstName} ${manager.lastName} je postavljen za menadžera.`);
        this.load(loc.id);
        this.loadCandidates();
      },
      error: (error) => {
        this.assigningUserId.set(null);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  /** [A2] Uklanjanje menadžera sa mesta. */
  removeManager(userId: number): void {
    const loc = this.location();
    if (!loc) {
      return;
    }

    this.assigningUserId.set(userId);
    this.clearMessages();

    this.managersService.remove(loc.id, userId).subscribe({
      next: () => {
        this.assigningUserId.set(null);
        this.infoMessage.set('Menadžer je uklonjen sa mesta.');
        this.load(loc.id);
        this.loadCandidates();
      },
      error: (error) => {
        this.assigningUserId.set(null);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  /** [K3] Logičko uklanjanje mesta — samo administrator. */
  deleteLocation(): void {
    const loc = this.location();
    if (!loc || !confirm(`Ukloniti mesto "${loc.name}"?`)) {
      return;
    }

    this.locationsService.remove(loc.id).subscribe({
      next: () => void this.router.navigate(['/mesta']),
      error: (error) => this.errorMessage.set(readApiError(error))
    });
  }

  /** [K4] Dogadjaji na ovom mestu; podrazumevano samo predstojeci [K3]. */
  loadEvents(locationId: number): void {
    this.eventsLoading.set(true);
    this.eventsService.byLocation(locationId, this.showPastEvents()).subscribe({
      next: (events) => {
        this.events.set(events);
        this.eventsLoading.set(false);
      },
      error: () => {
        this.events.set([]);
        this.eventsLoading.set(false);
      }
    });
  }

  togglePastEvents(): void {
    const loc = this.location();
    if (!loc) {
      return;
    }
    this.showPastEvents.update((value) => !value);
    this.loadEvents(loc.id);
  }

  eventImageUrl(event: Event): string {
    return this.eventsService.imageUrl(event);
  }

  eventTypeLabel(type: EventType): string {
    return EVENT_TYPE_LABELS[type] ?? type;
  }

  /** [K5] Utisci na ovom mestu. */
  loadReviews(locationId: number): void {
    this.reviewsLoading.set(true);
    this.reviewsService.byLocation(locationId).subscribe({
      next: (reviews) => {
        this.reviews.set(reviews);
        this.reviewsLoading.set(false);
      },
      error: () => {
        this.reviews.set([]);
        this.reviewsLoading.set(false);
      }
    });
  }

  /** Ocena date stavke na utisku, ili null ako je korisnik nije ocenio. */
  rateValue(review: Review, category: RateCategory): number | null {
    return review.rates[category] ?? null;
  }

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.infoMessage.set(null);
  }
}
