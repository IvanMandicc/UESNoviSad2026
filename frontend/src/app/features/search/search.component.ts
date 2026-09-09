import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { LocationSearchResult } from '../../core/models/search.models';
import { readApiError } from '../../core/services/api-error.util';
import { AuthService } from '../../core/services/auth.service';
import { LocationSearchService } from '../../core/services/location-search.service';

/**
 * [S1] Napredna pretraga mesta kroz Elasticsearch.
 * <p>
 * Tekstualna polja prihvataju posebne oblike unosa: "tačna fraza" pod
 * navodnicima, prefiks* sa zvezdicom i ~pojam sa tildom za tolerantnu pretragu.
 * Upit se pretprocesira analizatorom, pa radi nezavisno od velikog i malog
 * slova i od ćiriličnog ili latiničnog pisma.
 */
@Component({
  selector: 'app-search',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss'
})
export class SearchComponent {
  private readonly fb = inject(FormBuilder);
  private readonly searchService = inject(LocationSearchService);
  private readonly authService = inject(AuthService);

  readonly results = signal<LocationSearchResult[]>([]);
  readonly loading = signal(false);
  readonly searched = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(null);
  readonly reindexing = signal(false);
  readonly isAdmin = this.authService.isAdmin;

  /** Naslov iznad rezultata; menja se kada se prikazuju slična mesta. */
  readonly resultsTitle = signal('Rezultati');

  readonly form = this.fb.nonNullable.group({
    name: [''],
    description: [''],
    pdfContent: [''],
    minReviews: [null as number | null],
    maxReviews: [null as number | null],
    minPerformance: [null as number | null],
    maxPerformance: [null as number | null],
    minSoundAndLight: [null as number | null],
    maxSoundAndLight: [null as number | null],
    minSpace: [null as number | null],
    maxSpace: [null as number | null],
    minOverall: [null as number | null],
    maxOverall: [null as number | null],
    operator: ['AND' as 'AND' | 'OR'],
    sortBy: [''],
    sortDirection: ['asc' as 'asc' | 'desc']
  });

  search(): void {
    const values = this.form.getRawValue();

    this.loading.set(true);
    this.errorMessage.set(null);
    this.infoMessage.set(null);
    this.resultsTitle.set('Rezultati');

    this.searchService
      .search({
        name: values.name.trim() || null,
        description: values.description.trim() || null,
        pdfContent: values.pdfContent.trim() || null,
        minReviews: values.minReviews,
        maxReviews: values.maxReviews,
        minPerformance: values.minPerformance,
        maxPerformance: values.maxPerformance,
        minSoundAndLight: values.minSoundAndLight,
        maxSoundAndLight: values.maxSoundAndLight,
        minSpace: values.minSpace,
        maxSpace: values.maxSpace,
        minOverall: values.minOverall,
        maxOverall: values.maxOverall,
        operator: values.operator,
        sortBy: values.sortBy || null,
        sortDirection: values.sortDirection
      })
      .subscribe({
        next: (results) => {
          this.results.set(results);
          this.searched.set(true);
          this.loading.set(false);
        },
        error: (error) => {
          this.errorMessage.set(readApiError(error));
          this.loading.set(false);
        }
      });
  }

  /** [S1] Slična mesta na osnovu naziva, opisa i sadržaja PDF-a. */
  showSimilar(result: LocationSearchResult): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.searchService.similar(result.id).subscribe({
      next: (results) => {
        this.results.set(results);
        this.searched.set(true);
        this.resultsTitle.set(`Mesta slična mestu „${result.name}"`);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  reset(): void {
    this.form.reset({ operator: 'AND', sortDirection: 'asc' });
    this.results.set([]);
    this.searched.set(false);
    this.resultsTitle.set('Rezultati');
    this.errorMessage.set(null);
    this.infoMessage.set(null);
  }

  /** Ponovno indeksiranje — korisno kada se indeks raziđe sa bazom. */
  reindex(): void {
    this.reindexing.set(true);
    this.errorMessage.set(null);

    this.searchService.reindex().subscribe({
      next: (response) => {
        this.reindexing.set(false);
        this.infoMessage.set(response.message);
      },
      error: (error) => {
        this.reindexing.set(false);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  imageUrl(result: LocationSearchResult): string {
    return this.searchService.absoluteUrl(result.imageUrl) ?? '';
  }

  pdfUrl(result: LocationSearchResult): string | null {
    return this.searchService.absoluteUrl(result.pdfUrl);
  }
}
