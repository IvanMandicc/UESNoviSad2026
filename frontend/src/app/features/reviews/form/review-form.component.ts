import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Event } from '../../../core/models/event.models';
import { RATE_CATEGORIES, RateControl } from '../../../core/models/review.models';
import { readApiError } from '../../../core/services/api-error.util';
import { LocationsService } from '../../../core/services/locations.service';
import { ReviewsService } from '../../../core/services/reviews.service';

/**
 * [K5] Ostavljanje utiska na mesto.
 * <p>
 * Bira se događaj koji se održao na mestu, pa se ocenjuju stavke na skali 1-10.
 * Nije neophodno oceniti svaku stavku, ali bar jedna mora biti data.
 */
@Component({
  selector: 'app-review-form',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './review-form.component.html',
  styleUrl: './review-form.component.scss'
})
export class ReviewFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly reviewsService = inject(ReviewsService);
  private readonly locationsService = inject(LocationsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly categories = RATE_CATEGORIES;
  /** Skala 1-10 za dugmad ocenjivanja. */
  readonly scale = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  readonly locationId = signal<number>(0);
  readonly locationName = signal<string>('');
  readonly events = signal<Event[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    eventId: [null as number | null, [Validators.required]],
    performance: [null as number | null],
    soundAndLight: [null as number | null],
    space: [null as number | null],
    overall: [null as number | null],
    comment: ['']
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.locationId.set(id);
    this.load(id);
  }

  private load(locationId: number): void {
    this.loading.set(true);

    this.locationsService.get(locationId).subscribe({
      next: (location) => this.locationName.set(location.name),
      error: () => this.locationName.set('')
    });

    this.reviewsService.reviewableEvents(locationId).subscribe({
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

  /** Klik na istu ocenu je poništava — tako se stavka ostavlja neocenjena. */
  setRate(control: RateControl, value: number): void {
    const current = this.form.controls[control].value;
    this.form.controls[control].setValue(current === value ? null : value);
  }

  rateOf(control: RateControl): number | null {
    return this.form.controls[control].value;
  }

  get hasAnyRate(): boolean {
    const { performance, soundAndLight, space, overall } = this.form.getRawValue();
    return [performance, soundAndLight, space, overall].some((value) => value !== null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.hasAnyRate) {
      this.errorMessage.set('Ocenite bar jednu stavku.');
      return;
    }

    const values = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.reviewsService
      .create(this.locationId(), {
        eventId: values.eventId!,
        performance: values.performance,
        soundAndLight: values.soundAndLight,
        space: values.space,
        overall: values.overall,
        comment: values.comment.trim() || null
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/mesta', this.locationId()]);
        },
        error: (error) => {
          this.submitting.set(false);
          this.errorMessage.set(readApiError(error));
        }
      });
  }
}
