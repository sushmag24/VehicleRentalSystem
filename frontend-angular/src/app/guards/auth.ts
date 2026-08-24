import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth';

export const authGuard = (requiredRole?: string): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isAuthenticated(requiredRole)) {
      return true;
    }

    const currentRole = authService.getRole();
    if (currentRole) {
      // Authenticated but wrong role
      if (currentRole === 'ADMIN') {
        router.navigate(['/admin']);
      } else {
        router.navigate(['/customer']);
      }
    } else {
      // Not authenticated at all
      router.navigate(['/login']);
    }
    return false;
  };
};
