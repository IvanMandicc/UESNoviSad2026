import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  LOCATION_TYPE_LABELS,
  LOCATION_TYPES,
  Location,
  LocationType
} from '../../../core/models/location.models';
import { readApiError } from '../../../core/services/api-error.util';
import { LocationsService } from '../../../core/services/locations.service';

/** [K3] Dodavanje i izmena mesta — jedino administrator sistema. */
@Component({
  selector: 'app-location-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './location-form.component.html',
  styleUrl: './location-form.component.scss'
})
export class LocationFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly locationsService = inject(LocationsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly types = LOCATION_TYPES;
  readonly typeLabels = LOCATION_TYPE_LABELS;

  readonly submitting = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Postojeća slika pri izmeni; pri dodavanju je null. */
  readonly currentImageUrl = signal<string | null>(null);
  readonly locationId = signal<number | null>(null);
  readonly isEdit = signal(false);

  selectedFile: File | null = null;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    address: ['', [Validators.required, Validators.maxLength(300)]],
    type: ['KLUB' as LocationType, [Validators.required]],
    description: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.locationId.set(Number(idParam));
      this.isEdit.set(true);
      this.loadExisting(Number(idParam));
    }
  }

  private loadExisting(id: number): void {
    this.loading.set(true);
    this.locationsService.get(id).subscribe({
      next: (location: Location) => {
        this.form.patchValue({
          name: location.name,
          address: location.address,
          type: location.type,
          description: location.description
        });
        this.currentImageUrl.set(this.locationsService.imageUrl(location));
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Slika je obavezna samo pri dodavanju; pri izmeni se izostavlja da ostane stara.
    if (!this.isEdit() && !this.selectedFile) {
      this.errorMessage.set('Slika mesta je obavezna.');
      return;
    }

    const values = this.form.getRawValue();
    const payload = new FormData();
    payload.append('name', values.name);
    payload.append('address', values.address);
    payload.append('type', values.type);
    payload.append('description', values.description);
    if (this.selectedFile) {
      payload.append('image', this.selectedFile);
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const id = this.locationId();
    const request$ =
      this.isEdit() && id !== null
        ? this.locationsService.update(id, payload)
        : this.locationsService.create(payload);

    request$.subscribe({
      next: (location) => {
        this.submitting.set(false);
        void this.router.navigate(['/mesta', location.id]);
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
}
