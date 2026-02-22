import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./pages/request-list/request-list.component').then(m => m.RequestListComponent) },
  { path: 'requests/new', loadComponent: () => import('./pages/request-create/request-create.component').then(m => m.RequestCreateComponent) },
  { path: 'requests/:id', loadComponent: () => import('./pages/request-detail/request-detail.component').then(m => m.RequestDetailComponent) },
  { path: '**', redirectTo: '' }
];
