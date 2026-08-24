package com.vehiclerental.controller;

import com.vehiclerental.model.Reservation;
import com.vehiclerental.model.ReservationStatus;
import com.vehiclerental.model.User;
import com.vehiclerental.model.Vehicle;
import com.vehiclerental.service.ReservationService;
import com.vehiclerental.service.UserService;
import com.vehiclerental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only management endpoints.
 * In production these should be protected by JWT role-check middleware.
 *
 * Vehicles:
 *   GET    /api/admin/vehicles          — list all vehicles
 *   POST   /api/admin/vehicles          — add a new vehicle
 *   PUT    /api/admin/vehicles/{id}     — update vehicle details/status
 *   DELETE /api/admin/vehicles/{id}     — remove a vehicle
 *
 * Reservations:
 *   GET    /api/admin/reservations              — all reservations
 *   PUT    /api/admin/reservations/{id}/status  — update reservation status
 *
 * Users:
 *   GET    /api/admin/users             — list all registered users
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private VehicleService     vehicleService;
    @Autowired private ReservationService reservationService;
    @Autowired private UserService        userService;

    // ─────────────────────────────────────────────
    //  VEHICLE MANAGEMENT
    // ─────────────────────────────────────────────

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> addVehicle(@RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.addVehicle(vehicle));
    }

    /**
     * Partial update — only non-null fields in the request body are applied.
     * Useful for changing just the status (AVAILABLE → MAINTENANCE) or just the price.
     */
    @PutMapping("/vehicles/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id,
                                                  @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(Map.of("message", "Vehicle #" + id + " deleted successfully."));
    }

    // ─────────────────────────────────────────────
    //  RESERVATION MANAGEMENT
    // ─────────────────────────────────────────────

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    /**
     * Admin can manually transition reservation status.
     * Common use case: mark CONFIRMED → COMPLETED when vehicle is returned.
     *
     * @param status new status string (PENDING | CONFIRMED | CANCELLED | COMPLETED)
     */
    @PutMapping("/reservations/{id}/status")
    public ResponseEntity<Reservation> updateReservationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        ReservationStatus newStatus;
        try {
            newStatus = ReservationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: '" + status +
                    "'. Valid values: PENDING, CONFIRMED, CANCELLED, COMPLETED");
        }
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, newStatus));
    }

    // ─────────────────────────────────────────────
    //  USER MANAGEMENT (READ-ONLY)
    // ─────────────────────────────────────────────

    /** View all registered users. Passwords are @JsonIgnored. */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
