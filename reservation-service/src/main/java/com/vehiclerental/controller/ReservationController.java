package com.vehiclerental.controller;

import com.vehiclerental.dto.BookVehicleRequest;
import com.vehiclerental.dto.ReservationResponse;
import com.vehiclerental.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController

public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping("/api/reservations")
    public ResponseEntity<ReservationResponse> bookVehicle(
            @RequestBody BookVehicleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long customerId = null;
        if (userIdHeader != null) {
            customerId = Long.parseLong(userIdHeader);
        } else if (request.getCustomerId() != null) {
            customerId = request.getCustomerId();
        }
        if (customerId == null) {
            throw new RuntimeException("Customer ID is missing.");
        }
        return ResponseEntity.ok(reservationService.createReservation(customerId, request));
    }

    @GetMapping("/api/reservations/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @GetMapping("/api/reservations/customer/{customerId}")
    public ResponseEntity<List<ReservationResponse>> getCustomerReservations(@PathVariable Long customerId) {
        return ResponseEntity.ok(reservationService.getReservationsByCustomerId(customerId));
    }

    @PutMapping("/api/reservations/{id}/cancel")
    public ResponseEntity<?> cancelReservation(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleHeader) {
        Long currentUserId = null;
        if (userIdHeader != null) {
            currentUserId = Long.parseLong(userIdHeader);
        }
        ReservationResponse cancelled = reservationService.cancelReservation(id, currentUserId, userRoleHeader);
        return ResponseEntity.ok(Map.of(
                "message", "Reservation #" + id + " has been cancelled.",
                "status", cancelled.getStatus()
        ));
    }

    @GetMapping("/api/reservations/reserved-ids")
    public ResponseEntity<List<Long>> getReservedVehicleIds(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reservationService.getReservedVehicleIds(startDate, endDate));
    }

    @GetMapping("/api/admin/reservations")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @PutMapping("/api/admin/reservations/{id}/status")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable Long id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, status));
    }
}
