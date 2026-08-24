import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth';
import { ToastService } from '../services/toast';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html'
})
export class LoginComponent {
  activeTab: 'login' | 'register' = 'login';

  // Login inputs
  loginEmail = '';
  loginPassword = '';
  isLoggingIn = false;

  // Register inputs
  regName = '';
  regEmail = '';
  regPassword = '';
  regLicense = '';
  isRegistering = false;

  constructor(
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {
    // If already authenticated, redirect
    const role = this.authService.getRole();
    if (role && this.authService.getToken()) {
      this.router.navigate([role === 'ADMIN' ? '/admin' : '/customer']);
    }
  }

  showLogin(): void {
    this.activeTab = 'login';
  }

  showRegister(): void {
    this.activeTab = 'register';
  }

  login(): void {
    const email = this.loginEmail.trim();
    const password = this.loginPassword;

    if (!email || !password) {
      this.toastService.show('Please enter your email and password.', 'error');
      return;
    }

    this.isLoggingIn = true;

    this.authService.login({ email, password }).subscribe({
      next: (data) => {
        this.authService.saveSession(data);
        this.toastService.show('Logged in successfully!', 'success');
        this.router.navigate([data.role === 'ADMIN' ? '/admin' : '/customer']);
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Login failed. Check your credentials.';
        this.toastService.show(errorMsg, 'error');
        this.isLoggingIn = false;
      }
    });
  }

  /** Returns an error message string if password is invalid, or null if valid. */
  private validatePassword(password: string): string | null {
    if (password.length < 8) {
      return 'Password must be at least 8 characters.';
    }
    if (!/[A-Z]/.test(password)) {
      return 'Password must contain at least one uppercase letter (A-Z).';
    }
    if (!/[a-z]/.test(password)) {
      return 'Password must contain at least one lowercase letter (a-z).';
    }
    if (!/[0-9]/.test(password)) {
      return 'Password must contain at least one number (0-9).';
    }
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]/.test(password)) {
      return 'Password must contain at least one special character (e.g. !, @, #, $).';
    }
    return null;
  }

  register(): void {
    const name = this.regName.trim();
    const email = this.regEmail.trim();
    const password = this.regPassword;
    const licenseNumber = this.regLicense.trim();

    if (!name || !email || !password || !licenseNumber) {
      this.toastService.show('Please fill in all fields.', 'error');
      return;
    }

    const pwdError = this.validatePassword(password);
    if (pwdError) {
      this.toastService.show(pwdError, 'error');
      return;
    }

    this.isRegistering = true;

    this.authService.register({ name, email, password, licenseNumber }).subscribe({
      next: () => {
        this.toastService.show('Account created! Please sign in. 🎉', 'success');
        
        // Reset registration form
        this.regName = '';
        this.regPassword = '';
        this.regLicense = '';
        this.isRegistering = false;

        // Prefill login email and switch to login tab
        this.loginEmail = email;
        this.showLogin();
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Registration failed.';
        this.toastService.show(errorMsg, 'error');
        this.isRegistering = false;
      }
    });
  }
}
