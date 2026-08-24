package com.vehiclerental.controller;

import com.vehiclerental.dto.BookVehicleRequest;
import com.vehiclerental.model.Reservation;
import com.vehiclerental.model.ReservationStatus;
import com.vehiclerental.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Customer-facing reservation endpoints.
 *
 * POST /api/reservations                        — book a vehicle
 * GET  /api/reservations/customer/{customerId}  — customer's own bookings
 * GET  /api/reservations/{id}                   — single reservation detail
 * PUT  /api/reservations/{id}/cancel            — cancel a reservation
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    /** Create a new reservation (book a vehicle) */
    @PostMapping
    public ResponseEntity<Reservation> bookVehicle(@RequestBody BookVehicleRequest request) {
        Reservation reservation = reservationService.bookVehicle(
                request.getCustomerId(),
                request.getVehicleId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPaymentMethod(),
                request.getCouponCode()
        );
        return ResponseEntity.ok(reservation);
    }

    /** Get a single reservation by ID */
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    /** Get all reservations belonging to a specific customer */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Reservation>> getCustomerReservations(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(reservationService.getCustomerReservations(customerId));
    }

    /** Cancel a reservation — can be called by customer or admin */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        Reservation cancelled = reservationService.cancelReservation(id);
        return ResponseEntity.ok(Map.of(
                "message", "Reservation #" + id + " has been cancelled.",
                "status",  cancelled.getStatus().name()
        ));
    }
}
