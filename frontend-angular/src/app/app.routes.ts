import { Routes } from '@angular/router';
import { LoginComponent } from './login/login';
import { CustomerComponent } from './customer/customer';
import { AdminComponent } from './admin/admin';
import { authGuard } from './guards/auth';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'customer', component: CustomerComponent, canActivate: [authGuard('CUSTOMER')] },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard('ADMIN')] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];
