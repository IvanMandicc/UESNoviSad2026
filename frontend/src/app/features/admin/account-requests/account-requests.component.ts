import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AccountRequest } from '../../../core/models/auth.models';
import { AccountRequestsService } from '../../../core/services/account-requests.service';
import { readApiError } from '../../../core/services/api-error.util';

/** [A1] Administrator prihvata ili odbija pristigle zahteve za registraciju. */
@Component({
  selector: 'app-account-requests',
  imports: [DatePipe, FormsModule],
  templateUrl: './account-requests.component.html',
  styleUrl: './account-requests.component.scss'
})
export class AccountRequestsComponent {
  private readonly service = inject(AccountRequestsService);

  readonly requests = signal<AccountRequest[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(null);
  readonly showAll = signal(false);
  /** Id zahteva nad kojim je akcija u toku, da bismo iskljucili bas ta dugmad. */
  readonly busyId = signal<number | null>(null);
  /** Id zahteva za koji je otvorena forma za unos razloga odbijanja. */
  readonly rejectingId = signal<number | null>(null);
  rejectionReason = '';

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.service.list(this.showAll()).subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readApiError(error));
        this.loading.set(false);
      }
    });
  }

  toggleShowAll(): void {
    this.showAll.update((value) => !value);
    this.load();
  }

  approve(request: AccountRequest): void {
    this.busyId.set(request.id);
    this.clearMessages();

    this.service.approve(request.id).subscribe({
      next: (user) => {
        this.busyId.set(null);
        this.infoMessage.set(`Zahtev korisnika ${user.email} je prihvaćen i nalog je kreiran.`);
        this.load();
      },
      error: (error) => {
        this.busyId.set(null);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  startReject(request: AccountRequest): void {
    this.rejectingId.set(request.id);
    this.rejectionReason = '';
    this.clearMessages();
  }

  cancelReject(): void {
    this.rejectingId.set(null);
    this.rejectionReason = '';
  }

  confirmReject(request: AccountRequest): void {
    this.busyId.set(request.id);
    const reason = this.rejectionReason.trim() || null;

    this.service.reject(request.id, reason).subscribe({
      next: (rejected) => {
        this.busyId.set(null);
        this.rejectingId.set(null);
        this.rejectionReason = '';
        this.infoMessage.set(`Zahtev korisnika ${rejected.email} je odbijen.`);
        this.load();
      },
      error: (error) => {
        this.busyId.set(null);
        this.errorMessage.set(readApiError(error));
      }
    });
  }

  /** Naziv statusa na srpskom, za prikaz u tabeli. */
  statusLabel(request: AccountRequest): string {
    switch (request.status) {
      case 'PENDING':
        return 'Na čekanju';
      case 'APPROVED':
        return 'Prihvaćen';
      default:
        return 'Odbijen';
    }
  }

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.infoMessage.set(null);
  }
}
