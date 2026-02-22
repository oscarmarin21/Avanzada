import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: '', pathMatch: 'full', canActivate: [authGuard], loadComponent: () => import('./pages/request-list/request-list.component').then(m => m.RequestListComponent) },
  { path: 'requests/new', canActivate: [authGuard], loadComponent: () => import('./pages/request-create/request-create.component').then(m => m.RequestCreateComponent) },
  { path: 'requests/:id', canActivate: [authGuard], loadComponent: () => import('./pages/request-detail/request-detail.component').then(m => m.RequestDetailComponent) },
  { path: '**', redirectTo: '' }
];
