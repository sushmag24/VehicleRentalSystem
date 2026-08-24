import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private api = 'http://localhost:8080/api';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getVehiclesByCategory(category: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/vehicles/category/${category}`, {
      headers: this.authService.getHeaders()
    });
  }

  getAvailableVehicles(category: string, startDate: string, endDate: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.api}/vehicles/available?startDate=${startDate}&endDate=${endDate}&category=${category}`,
      { headers: this.authService.getHeaders() }
    );
  }

  getAdminVehicles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/admin/vehicles`, {
      headers: this.authService.getHeaders()
    });
  }

  addVehicle(vehicle: any): Observable<any> {
    return this.http.post(`${this.api}/admin/vehicles`, vehicle, {
      headers: this.authService.getHeaders()
    });
  }

  updateVehicle(id: number, vehicle: any): Observable<any> {
    return this.http.put(`${this.api}/admin/vehicles/${id}`, vehicle, {
      headers: this.authService.getHeaders()
    });
  }

  deleteVehicle(id: number): Observable<any> {
    return this.http.delete(`${this.api}/admin/vehicles/${id}`, {
      headers: this.authService.getHeaders()
    });
  }
}
