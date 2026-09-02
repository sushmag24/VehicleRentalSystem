import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  createReservation(booking: any): Observable<any> {
    return this.http.post(`${this.api}/reservations`, booking, {
      headers: this.authService.getHeaders()
    });
  }

  getCustomerReservations(customerId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/reservations/customer/${customerId}`, {
      headers: this.authService.getHeaders()
    });
  }

  cancelReservation(id: number): Observable<any> {
    return this.http.put(`${this.api}/reservations/${id}/cancel`, {}, {
      headers: this.authService.getHeaders()
    });
  }

  getAdminReservations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/admin/reservations`, {
      headers: this.authService.getHeaders()
    });
  }

  updateReservationStatus(id: number, status: string): Observable<any> {
    return this.http.put(
      `${this.api}/admin/reservations/${id}/status?status=${status}`,
      {},
      { headers: this.authService.getHeaders() }
    );
  }
}
