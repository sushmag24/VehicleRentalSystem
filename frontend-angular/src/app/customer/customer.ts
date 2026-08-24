import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { VehicleService } from '../services/vehicle';
import { ReservationService } from '../services/reservation';
import { ToastService } from '../services/toast';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customer.html'
})
export class CustomerComponent implements OnInit {
  activeTab: 'panel-vehicles' | 'panel-bookings' = 'panel-vehicles';
  userName = 'Customer';
  currentCategory: 'FOUR_WHEELER' | 'TWO_WHEELER' | null = null;
  todayStr = '';

  // Smart Recommendation inputs & results
  tripStart = '';
  tripEnd = '';
  tripDistance: number | null = null;
  recommendationResult: { type: 'FOUR_WHEELER' | 'TWO_WHEELER'; title: string; reason: string } | null = null;

  // Search filter inputs
  searchStart = '';
  searchEnd = '';

  // Vehicle list & status
  vehicles: any[] = [];
  isLoadingVehicles = false;

  // Bookings list
  reservations: any[] = [];
  isLoadingReservations = false;

  // Booking Modal State
  isBookingModalOpen = false;
  selectedVehicle: any = null;
  bookStartDate = '';
  bookEndDate = '';
  bookCoupon = '';
  bookPayment = '';
  isSubmittingBooking = false;

  constructor(
    private authService: AuthService,
    private vehicleService: VehicleService,
    private reservationService: ReservationService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Check Auth
    if (!this.authService.isAuthenticated('CUSTOMER')) {
      this.logout();
      return;
    }
    const name = this.authService.getName();
    if (name) {
      this.userName = name;
    }

    this.todayStr = new Date().toISOString().split('T')[0];
    this.loadMyReservations();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  switchTab(tab: 'panel-vehicles' | 'panel-bookings'): void {
    this.activeTab = tab;
  }

  // --- Category Selection Screen ---
  selectCategory(category: 'FOUR_WHEELER' | 'TWO_WHEELER'): void {
    this.currentCategory = category;
    this.loadAvailableVehicles();
  }

  backToSelection(): void {
    this.currentCategory = null;
    this.vehicles = [];
  }

  getRecommendation(): void {
    const start = this.tripStart.trim();
    const end = this.tripEnd.trim();
    const distance = this.tripDistance;

    if (!distance && !start && !end) {
      this.toastService.show('Please fill in distance and locations.', 'error');
      return;
    }

    const distVal = distance || 0;
    let rec: 'FOUR_WHEELER' | 'TWO_WHEELER' = 'TWO_WHEELER';
    let reason = '';

    if (distVal >= 50) {
      rec = 'FOUR_WHEELER';
      reason = `For a distance of ${distVal}km, a Four Wheeler is recommended for a comfortable journey.`;
    } else {
      rec = 'TWO_WHEELER';
      reason = `For a distance of ${distVal}km, a Two Wheeler is fast and economical.`;
    }

    this.recommendationResult = {
      type: rec,
      title: `We recommend a ${rec === 'FOUR_WHEELER' ? 'Four Wheeler' : 'Two Wheeler'}`,
      reason: reason
    };
  }

  // --- Vehicle Listing & Searching ---
  loadAvailableVehicles(): void {
    if (!this.currentCategory) return;

    this.isLoadingVehicles = true;

    // Validate dates if both provided
    if (this.searchStart && this.searchEnd && this.searchEnd < this.searchStart) {
      this.toastService.show('End date must be on or after start date.', 'error');
      this.isLoadingVehicles = false;
      this.vehicles = [];
      return;
    }

    let req$;
    if (this.searchStart && this.searchEnd) {
      req$ = this.vehicleService.getAvailableVehicles(this.currentCategory, this.searchStart, this.searchEnd);
    } else {
      req$ = this.vehicleService.getVehiclesByCategory(this.currentCategory);
    }

    req$.subscribe({
      next: (data) => {
        // filter on frontend just in case
        this.vehicles = data.filter((v: any) => v.category === this.currentCategory);
        this.isLoadingVehicles = false;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Failed to load vehicles.', 'error');
        this.isLoadingVehicles = false;
      }
    });
  }

  clearSearch(): void {
    this.searchStart = '';
    this.searchEnd = '';
    this.loadAvailableVehicles();
  }

  // --- Booking Modal Actions ---
  openBookingModal(vehicle: any): void {
    this.selectedVehicle = vehicle;
    this.bookCoupon = '';
    this.bookPayment = '';
    
    // Pre-fill from search bar
    this.bookStartDate = this.searchStart || '';
    this.bookEndDate = this.searchEnd || '';

    this.isBookingModalOpen = true;
  }

  closeBookingModal(): void {
    this.isBookingModalOpen = false;
    this.selectedVehicle = null;
  }

  get pricePreviewText(): string {
    if (!this.bookStartDate || !this.bookEndDate) {
      return 'Select dates to see total cost.';
    }

    const days = Math.floor((new Date(this.bookEndDate).getTime() - new Date(this.bookStartDate).getTime()) / 86400000) + 1;
    if (days < 1) {
      return '<span style="color:var(--danger)">End date must be after start date.</span>';
    }

    const total = days * this.selectedVehicle.pricePerDay;
    return `${days} day${days > 1 ? 's' : ''} × ₹${this.selectedVehicle.pricePerDay.toLocaleString('en-IN')} = <span class="preview-total">₹${Math.round(total).toLocaleString('en-IN')}</span>`;
  }

  submitBooking(): void {
    if (!this.bookStartDate || !this.bookEndDate) {
      this.toastService.show('Please select both dates.', 'error');
      return;
    }
    if (this.bookEndDate < this.bookStartDate) {
      this.toastService.show('End date must be after start date.', 'error');
      return;
    }
    if (!this.bookPayment) {
      this.toastService.show('Please select a payment method.', 'error');
      return;
    }
    if (!this.selectedVehicle) {
      this.toastService.show('No vehicle selected.', 'error');
      return;
    }

    this.isSubmittingBooking = true;
    const customerId = parseInt(this.authService.getUserId() || '0');

    this.reservationService.createReservation({
      customerId: customerId,
      vehicleId: this.selectedVehicle.id,
      startDate: this.bookStartDate,
      endDate: this.bookEndDate,
      paymentMethod: this.bookPayment,
      couponCode: this.bookCoupon.trim()
    }).subscribe({
      next: () => {
        this.closeBookingModal();
        this.toastService.show('Vehicle booked successfully! 🎉', 'success');
        this.loadAvailableVehicles();
        this.loadMyReservations();
        this.switchTab('panel-bookings');
        this.isSubmittingBooking = false;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Booking failed.', 'error');
        this.isSubmittingBooking = false;
      }
    });
  }

  // --- Bookings History Actions ---
  loadMyReservations(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    this.isLoadingReservations = true;
    this.reservationService.getCustomerReservations(userId).subscribe({
      next: (data) => {
        this.reservations = data;
        this.isLoadingReservations = false;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Failed to load reservations history.', 'error');
        this.isLoadingReservations = false;
      }
    });
  }

  canCancel(status: string): boolean {
    return status === 'CONFIRMED' || status === 'PENDING';
  }

  cancelReservation(id: number): void {
    if (!confirm('Cancel this reservation? This cannot be undone.')) return;

    this.reservationService.cancelReservation(id).subscribe({
      next: () => {
        this.toastService.show('Reservation cancelled.', 'warning');
        this.loadMyReservations();
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Cancellation failed.', 'error');
      }
    });
  }
}
