import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
    title: 'Novi Sad'
  },
  {
    // [K2] Prijava na sistem.
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent),
    title: 'Prijava'
  },
  {
    // [K1] Zahtev za registraciju.
    path: 'registracija',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
    title: 'Zahtev za registraciju'
  },
  {
    // [A1] Obrada zahteva za registraciju.
    path: 'admin/zahtevi',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/account-requests/account-requests.component').then(
        (m) => m.AccountRequestsComponent
      ),
    title: 'Zahtevi za registraciju'
  },
  {
    // [K3] Pregled mesta.
    path: 'mesta',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/locations/list/location-list.component').then((m) => m.LocationListComponent),
    title: 'Mesta'
  },
  {
    // [K3] Dodavanje mesta - samo administrator.
    path: 'mesta/novo',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/locations/form/location-form.component').then((m) => m.LocationFormComponent),
    title: 'Novo mesto'
  },
  {
    // [K3] Izmena svih podataka mesta - samo administrator.
    path: 'mesta/:id/izmena',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/locations/form/location-form.component').then((m) => m.LocationFormComponent),
    title: 'Izmena mesta'
  },
  {
    // [K3] Stranica mesta, [A2] upravljanje menadzerima.
    path: 'mesta/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/locations/detail/location-detail.component').then((m) => m.LocationDetailComponent),
    title: 'Mesto'
  },
  {
    // [K4] Dodavanje dogadjaja na mesto - menadzer mesta (ili administrator).
    path: 'mesta/:locationId/dogadjaji/novi',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/events/form/event-form.component').then((m) => m.EventFormComponent),
    title: 'Novi dogadjaj'
  },
  {
    // [K4] Izmena dogadjaja.
    path: 'dogadjaji/:id/izmena',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/events/form/event-form.component').then((m) => m.EventFormComponent),
    title: 'Izmena dogadjaja'
  },
  {
    // [K4] Stranica dogadjaja.
    path: 'dogadjaji/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/events/detail/event-detail.component').then((m) => m.EventDetailComponent),
    title: 'Dogadjaj'
  },
  {
    // [K5] Ostavljanje utiska na mesto.
    path: 'mesta/:id/utisak',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reviews/form/review-form.component').then((m) => m.ReviewFormComponent),
    title: 'Ostavi utisak'
  },
  {
    // [K6] Stranica za dogadjaje: danasnji dogadjaji sa svih mesta + filtriranje.
    path: 'dogadjaji',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/events/list/event-list.component').then((m) => m.EventListComponent),
    title: 'Dogadjaji'
  },
  {
    // [K9]/[K10] Profil korisnika.
    path: 'profil',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then((m) => m.ProfileComponent),
    title: 'Moj profil'
  },
  { path: '**', redirectTo: '' }
];
