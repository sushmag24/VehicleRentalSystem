import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private api = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUserId(): string | null {
    return localStorage.getItem('userId');
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  getName(): string | null {
    return localStorage.getItem('name');
  }

  getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.getToken()}`
    });
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.api}/auth/login`, credentials);
  }

  register(user: any): Observable<any> {
    return this.http.post(`${this.api}/auth/register`, user);
  }

  saveSession(data: any): void {
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', data.role);
    localStorage.setItem('userId', data.userId.toString());
    if (data.name) {
      localStorage.setItem('name', data.name);
    } else if (data.email) {
      const nameVal = data.email.split('@')[0];
      localStorage.setItem('name', nameVal.charAt(0).toUpperCase() + nameVal.slice(1));
    }
  }

  logout(): void {
    localStorage.clear();
  }

  isAuthenticated(requiredRole?: string): boolean {
    const role = this.getRole();
    const token = this.getToken();
    if (!role || !token) {
      return false;
    }
    if (requiredRole && role !== requiredRole) {
      return false;
    }
    return true;
  }
}
