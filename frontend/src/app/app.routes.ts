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
  { path: '**', redirectTo: '' }
];
