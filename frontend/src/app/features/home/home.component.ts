import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * Privremena pocetna stranica. Pravi sadrzaj (danasnji dogadjaji,
 * najpopularnija mesta, najskoriji utisci) dolazi sa [K8].
 */
@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  private readonly authService = inject(AuthService);

  readonly user = this.authService.user;
  readonly isAdmin = this.authService.isAdmin;
}
