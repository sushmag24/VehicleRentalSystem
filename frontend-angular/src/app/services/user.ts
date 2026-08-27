import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private api = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
    ? 'http://localhost:8080/api'
    : '/api';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getAdminUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/admin/users`, {
      headers: this.authService.getHeaders()
    });
  }
}
