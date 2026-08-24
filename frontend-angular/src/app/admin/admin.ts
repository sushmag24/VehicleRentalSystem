import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { VehicleService } from '../services/vehicle';
import { ReservationService } from '../services/reservation';
import { UserService } from '../services/user';
import { ToastService } from '../services/toast';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.html'
})
export class AdminComponent implements OnInit {
  activeTab: 'panel-vehicles' | 'panel-reservations' | 'panel-users' = 'panel-vehicles';

  // Stats
  statVehiclesCount = 0;
  statReservationsCount = 0;
  statUsersCount = 0;
  statAvailableCount = 0;

  // Vehicles lists & loader
  vehicles: any[] = [];
  isLoadingVehicles = false;

  // Add Vehicle form fields
  vName = '';
  vCategory = 'FOUR_WHEELER';
  vType = 'Sedan';
  vPrice: number | null = null;
  vRating = 4.5;
  vImage = '';
  vStatus = 'AVAILABLE';
  isAddingVehicle = false;

  // Edit Vehicle modal state
  isEditModalOpen = false;
  editId: number | null = null;
  editName = '';
  editCategory = 'FOUR_WHEELER';
  editType = 'Sedan';
  editPrice: number | null = null;
  editRating = 4.5;
  editImage = '';
  editStatus = 'AVAILABLE';
  isSavingEdit = false;

  // Reservations list & loader
  reservations: any[] = [];
  isLoadingReservations = false;

  // Users list & loader
  users: any[] = [];
  isLoadingUsers = false;

  constructor(
    private authService: AuthService,
    private vehicleService: VehicleService,
    private reservationService: ReservationService,
    private userService: UserService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated('ADMIN')) {
      this.logout();
      return;
    }

    this.loadAdminVehicles();
    this.loadAdminReservations();
    this.loadAdminUsers();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  switchTab(tab: 'panel-vehicles' | 'panel-reservations' | 'panel-users'): void {
    this.activeTab = tab;
  }

  // --- Vehicles management ---
  loadAdminVehicles(): void {
    this.isLoadingVehicles = true;
    this.vehicleService.getAdminVehicles().subscribe({
      next: (data) => {
        this.vehicles = data;
        this.isLoadingVehicles = false;

        // Calculate counts
        this.statVehiclesCount = this.vehicles.length;
        this.statAvailableCount = this.vehicles.filter(v => v.status === 'AVAILABLE').length;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Failed to load vehicles.', 'error');
        this.isLoadingVehicles = false;
      }
    });
  }

  addVehicle(): void {
    const name = this.vName.trim();
    const price = this.vPrice;

    if (!name || !price) {
      this.toastService.show('Please fill in Name and Price.', 'error');
      return;
    }

    this.isAddingVehicle = true;

    this.vehicleService.addVehicle({
      name,
      category: this.vCategory,
      type: this.vType,
      pricePerDay: price,
      rating: this.vRating,
      imageUrl: this.vImage,
      status: this.vStatus
    }).subscribe({
      next: (data) => {
        this.toastService.show(`"${name}" added successfully! ✅`, 'success');
        
        // Reset form
        this.vName = '';
        this.vPrice = null;
        this.vRating = 4.5;
        this.vImage = '';
        this.vStatus = 'AVAILABLE';
        this.isAddingVehicle = false;

        this.loadAdminVehicles();
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Failed to add vehicle.', 'error');
        this.isAddingVehicle = false;
      }
    });
  }

  openEditModal(v: any): void {
    this.editId = v.id;
    this.editName = v.name;
    this.editCategory = v.category;
    this.editType = v.type;
    this.editPrice = v.pricePerDay;
    this.editRating = v.rating || 4.5;
    this.editImage = v.imageUrl || '';
    this.editStatus = v.status;
    this.isEditModalOpen = true;
  }

  closeEditModal(): void {
    this.isEditModalOpen = false;
    this.editId = null;
  }

  saveVehicleEdit(): void {
    if (!this.editId) return;
    const name = this.editName.trim();
    const price = this.editPrice;

    if (!name || !price) {
      this.toastService.show('Name and Price are required.', 'error');
      return;
    }

    this.isSavingEdit = true;

    this.vehicleService.updateVehicle(this.editId, {
      name,
      category: this.editCategory,
      type: this.editType,
      pricePerDay: price,
      rating: this.editRating,
      imageUrl: this.editImage,
      status: this.editStatus
    }).subscribe({
      next: () => {
        this.closeEditModal();
        this.toastService.show('Vehicle updated! ✅', 'success');
        this.loadAdminVehicles();
        this.isSavingEdit = false;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Update failed.', 'error');
        this.isSavingEdit = false;
      }
    });
  }

  deleteVehicle(id: number): void {
    if (!confirm(`Delete vehicle #${id}? This cannot be undone.`)) return;

    this.vehicleService.deleteVehicle(id).subscribe({
      next: () => {
        this.toastService.show('Vehicle deleted.', 'warning');
        this.loadAdminVehicles();
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Delete failed.', 'error');
      }
    });
  }

  // --- Reservations management ---
  loadAdminReservations(): void {
    this.isLoadingReservations = true;
    this.reservationService.getAdminReservations().subscribe({
      next: (data) => {
        this.reservations = data;
        this.isLoadingReservations = false;
        this.statReservationsCount = this.reservations.length;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Failed to load reservations.', 'error');
        this.isLoadingReservations = false;
      }
    });
  }

  updateReservationStatus(id: number, status: string): void {
    if (!status) return;

    this.reservationService.updateReservationStatus(id, status).subscribe({
      next: () => {
        this.toastService.show(`Reservation #${id} → ${status}`, 'success');
        this.loadAdminReservations();
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Update status failed.', 'error');
      }
    });
  }

  // --- Users view ---
  loadAdminUsers(): void {
    this.isLoadingUsers = true;
    this.userService.getAdminUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.isLoadingUsers = false;
        this.statUsersCount = this.users.filter(u => u.role === 'CUSTOMER').length;
      },
      error: (err) => {
        this.toastService.show(err.error?.error || 'Failed to load users.', 'error');
        this.isLoadingUsers = false;
      }
    });
  }
}
