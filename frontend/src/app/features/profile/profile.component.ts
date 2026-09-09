import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { LOCATION_TYPE_LABELS, LocationType } from '../../core/models/location.models';
import { Profile } from '../../core/models/profile.models';
import { RATE_CATEGORIES, RateCategory, Review } from '../../core/models/review.models';
import { readApiError } from '../../core/services/api-error.util';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';

/** Polje je neispravno tek kada ga je korisnik dodirnuo. */
function isInvalid(control: AbstractControl): boolean {
  return control.invalid && (control.dirty || control.touched);
}

/**
 * [K10] Profil korisnika: dodatni podaci, slika, spisak utisaka i mesta
 * na kojima je menadžer. [K9] Promena lozinke.
 */
@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);

  readonly rateCategories = RATE_CATEGORIES;

  readonly profile = signal<Profile | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(null);

  readonly savingProfile = signal(false);
  readonly uploadingImage = signal(false);
  readonly changingPassword = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly passwordSuccess = signal<string | null>(null);

  selectedFile: File | null = null;

  readonly profileForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    city: [''],
    phone: ['']
  });

  /** [K9] Prvo trenutna lozinka, pa dva puta nova. */
  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64)]],
    confirmPassword: ['', [Validators.required]]
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.profileService.me().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileForm.patchValue({
          firstName: profile.user.firstName,
          lastName: profile.user.lastName,
          city: profile.user.city ?? '',
          phone: profile.user.phone ?? ''
        });
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  imageUrl(): string | null {
    const profile = this.profile();
    return profile ? this.profileService.imageUrl(profile.user) : null;
  }

  onFileSelected(event: globalThis.Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  /** [K10] Promena slike profila. */
  uploadImage(): void {
    if (!this.selectedFile) {
      this.errorMessage.set('Izaberite sliku.');
      return;
    }

    this.uploadingImage.set(true);
    this.clearMessages();

    this.profileService.updateImage(this.selectedFile).subscribe({
      next: () => {
        this.uploadingImage.set(false);
        this.selectedFile = null;
        this.infoMessage.set('Slika profila je promenjena.');
        this.load();
      },
      error: (error) => {
        this.uploadingImage.set(false);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  /** [K10] Promena dodatnih podataka na profilu. */
  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const values = this.profileForm.getRawValue();
    this.savingProfile.set(true);
    this.clearMessages();

    this.profileService
      .updateProfile({
        firstName: values.firstName,
        lastName: values.lastName,
        city: values.city.trim() || null,
        phone: values.phone.trim() || null
      })
      .subscribe({
        next: () => {
          this.savingProfile.set(false);
          this.infoMessage.set('Podaci su sačuvani.');
          this.load();
        },
        error: (error) => {
          this.savingProfile.set(false);
          this.errorMessage.set(readApiError(error));
        }
      });
  }

  /** [K9] Promena lozinke. */
  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const values = this.passwordForm.getRawValue();
    if (values.newPassword !== values.confirmPassword) {
      this.passwordError.set('Nova lozinka i potvrda se ne poklapaju.');
      return;
    }

    this.changingPassword.set(true);
    this.passwordError.set(null);
    this.passwordSuccess.set(null);

    this.profileService.changePassword(values).subscribe({
      next: (response) => {
        this.changingPassword.set(false);
        this.passwordForm.reset();
        this.passwordSuccess.set(response.message);
      },
      error: (error) => {
        this.changingPassword.set(false);
        this.passwordError.set(readApiError(error));
      }
    });
  }

  rateValue(review: Review, category: RateCategory): number | null {
    return review.rates[category] ?? null;
  }

  locationTypeLabel(type: LocationType): string {
    return LOCATION_TYPE_LABELS[type] ?? type;
  }

  logout(): void {
    this.authService.logout();
  }

  hasError(control: 'firstName' | 'lastName' | 'city' | 'phone'): boolean {
    return isInvalid(this.profileForm.controls[control]);
  }

  hasPasswordError(control: 'currentPassword' | 'newPassword' | 'confirmPassword'): boolean {
    return isInvalid(this.passwordForm.controls[control]);
  }

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.infoMessage.set(null);
  }
}
