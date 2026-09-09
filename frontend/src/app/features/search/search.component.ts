import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { LocationSearchResult } from '../../core/models/search.models';
import { readApiError } from '../../core/services/api-error.util';
import { AuthService } from '../../core/services/auth.service';
import { LocationSearchService } from '../../core/services/location-search.service';

/**
 * [S1] Pretraga mesta kroz Elasticsearch: po nazivu, opisu, sadržaju PDF-a
 * i opsegu broja utisaka. Analizator radi nezavisno od velikog i malog slova
 * i od ćiriličnog ili latiničnog pisma.
 * <p>
 * BooleanQuery (AND/OR), PhraseQuery/PrefixQuery/FuzzyQuery, opseg ocene po
 * kategorijama, sortiranje po nazivu, dinamički sažetak i „slična mesta" nisu
 * deo ovog prikaza — backend ih ima implementirane (LocationSearchService),
 * samo nisu izloženi ovde jer nisu bili traženi.
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

  readonly form = this.fb.nonNullable.group({
    name: [''],
    description: [''],
    pdfContent: [''],
    minReviews: [null as number | null],
    maxReviews: [null as number | null]
  });

  search(): void {
    const values = this.form.getRawValue();

    this.loading.set(true);
    this.errorMessage.set(null);
    this.infoMessage.set(null);

    this.searchService
      .search({
        name: values.name.trim() || null,
        description: values.description.trim() || null,
        pdfContent: values.pdfContent.trim() || null,
        minReviews: values.minReviews,
        maxReviews: values.maxReviews
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

  reset(): void {
    this.form.reset();
    this.results.set([]);
    this.searched.set(false);
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

  reviewCountLabel(count: number | null): string {
    const value = count ?? 0;
    return value === 1 ? '1 utisak' : `${value} utisaka`;
  }
}
