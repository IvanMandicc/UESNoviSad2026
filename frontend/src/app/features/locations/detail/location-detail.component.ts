import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { User } from '../../../core/models/auth.models';
import {
  LOCATION_TYPE_LABELS,
  LOCATION_TYPES,
  Location,
  LocationType
} from '../../../core/models/location.models';
import { readApiError } from '../../../core/services/api-error.util';
import { AuthService } from '../../../core/services/auth.service';
import { LocationsService } from '../../../core/services/locations.service';
import { ManagersService } from '../../../core/services/managers.service';

/**
 * [K3] Stranica mesta. Predstojeći događaji [K4] i prosečna ocena [K5]
 * su prikazani kao prazne sekcije dok ti zahtevi ne budu implementirani.
 * [A2] Administrator ovde dodaje i uklanja menadžere mesta.
 */
@Component({
  selector: 'app-location-detail',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './location-detail.component.html',
  styleUrl: './location-detail.component.scss'
})
export class LocationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly locationsService = inject(LocationsService);
  private readonly managersService = inject(ManagersService);
  private readonly authService = inject(AuthService);

  readonly types = LOCATION_TYPES;
  readonly typeLabels = LOCATION_TYPE_LABELS;

  readonly location = signal<Location | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(null);
  readonly editingAttributes = signal(false);
  readonly savingAttributes = signal(false);

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

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.infoMessage.set(null);
  }
}
