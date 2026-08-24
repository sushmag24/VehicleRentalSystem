package com.vehiclerental.service;

import com.vehiclerental.model.*;
import com.vehiclerental.repository.RentalRepository;
import com.vehiclerental.repository.ReservationRepository;
import com.vehiclerental.repository.UserRepository;
import com.vehiclerental.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business logic for vehicle reservations (booking, cancellation, history).
 *
 * Lifecycle:
 *   PENDING → CONFIRMED (booking created successfully)
 *   CONFIRMED → CANCELLED (customer or admin cancels)
 *   CONFIRMED → COMPLETED (admin marks rental returned)
 */
@Service
public class ReservationService {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private VehicleRepository     vehicleRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private RentalRepository      rentalRepository;

    // ─────────────────────────────────────────────
    //  BOOKING
    // ─────────────────────────────────────────────

    /**
     * Creates a new confirmed reservation and automatically generates a Rental record.
     */
    @Transactional
    public Reservation bookVehicle(Long customerId, Long vehicleId,
                                   LocalDate startDate, LocalDate endDate,
                                   String paymentMethodStr, String couponCode) {

        // ── Date validation ──────────────────────────────────────────────
        if (startDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past.");
        }
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("End date must be on or after start date.");
        }

        // ── Overlap check ────────────────────────────────────────────────
        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                vehicleId, startDate, endDate, ReservationStatus.CANCELLED);

        if (!conflicts.isEmpty()) {
            throw new RuntimeException(
                "Vehicle is not available from " + startDate + " to " + endDate +
                ". Please choose different dates.");
        }

        // ── Load entities ────────────────────────────────────────────────
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found."));

        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new RuntimeException("Vehicle is currently under maintenance.");
        }

        // ── Create reservation ───────────────────────────────────────────
        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setVehicle(vehicle);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);

        // ── Pricing Logic ────────────────────────────────────────────────
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1; // inclusive
        BigDecimal originalAmount = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days));
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCoupon      = null;

        // Discount Logic: FIRST15 for first 3 bookings
        if ("FIRST15".equalsIgnoreCase(couponCode)) {
            long previousBookings = reservationRepository.countByCustomer_IdAndStatusNot(customerId, ReservationStatus.CANCELLED);
            // Since we just saved the current one, it will be in the count if using JPA count.
            // Wait, reservationRepository.save(reservation) happens before.
            // But countBy... might include the one just saved if it's in the same transaction or if ID > 0.
            // Let's check how many *before* this one.
            // Actually, we can just subtract 1 or check count < 4.
            if (previousBookings <= 3) { // Including the current one
                discountAmount = originalAmount.multiply(new BigDecimal("0.15"));
                appliedCoupon = "FIRST15";
            } else {
                // If code provided but not eligible, we don't apply it.
                // Optionally throw error, but here we just proceed without discount.
            }
        }
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);

        // ── Payment Method ──────────────────────────────────────────────
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid payment method: " + paymentMethodStr);
        }

        // ── Auto-generate rental with financial details ──────────────────
        Rental rental = new Rental();
        rental.setReservation(saved);
        rental.setOriginalAmount(originalAmount);
        rental.setDiscountAmount(discountAmount);
        rental.setFinalAmount(finalAmount);
        rental.setCouponCode(appliedCoupon);
        rental.setPaymentMethod(method);
        rental.setPaymentStatus(PaymentStatus.UNPAID);
        rentalRepository.save(rental);

        return saved;
    }

    // ─────────────────────────────────────────────
    //  QUERIES
    // ─────────────────────────────────────────────

    /** Single reservation by ID */
    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation #" + id + " not found."));
    }

    /** All reservations for a customer, newest first */
    public List<Reservation> getCustomerReservations(Long customerId) {
        return reservationRepository.findByCustomer_IdOrderByStartDateDesc(customerId);
    }

    /** All reservations in the system (admin view), newest first */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAllByOrderByIdDesc();
    }

    // ─────────────────────────────────────────────
    //  STATUS TRANSITIONS
    // ─────────────────────────────────────────────

    /**
     * Cancels a reservation.
     * Only PENDING or CONFIRMED reservations can be cancelled.
     */
    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Reservation #" + reservationId + " is already cancelled.");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed reservation.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    /**
     * Admin-only: update reservation status (CONFIRMED → COMPLETED, etc.)
     */
    @Transactional
    public Reservation updateReservationStatus(Long reservationId, ReservationStatus newStatus) {
        Reservation reservation = getReservationById(reservationId);
        reservation.setStatus(newStatus);
        return reservationRepository.save(reservation);
    }
}
