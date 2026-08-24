package com.vehiclerental.controller;

import com.vehiclerental.dto.PaymentRequest;
import com.vehiclerental.model.PaymentMethod;
import com.vehiclerental.model.PaymentStatus;
import com.vehiclerental.model.Rental;
import com.vehiclerental.repository.RentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private RentalRepository rentalRepository;

    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request) {
        BigDecimal originalAmount = request.getOriginalAmount();
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCoupon = null;

        // Apply FIRST15 discount of 15% — valid for first 3 SUCCESSFUL payments only.
        // We count actual DB records where coupon was applied and payment is NOT REFUNDED.
        if ("FIRST15".equalsIgnoreCase(request.getCouponCode())) {
            long successfulCouponUses = rentalRepository.countByCouponCodeAndPaymentStatusNot(
                    "FIRST15", PaymentStatus.REFUNDED);

            if (successfulCouponUses >= 3) {
                // Coupon has reached its 3-use limit — reject with clear message
                return ResponseEntity.badRequest().body(
                        java.util.Map.of("error",
                                "Coupon FIRST15 has expired. It is valid for the first 3 successful payments only."));
            }

            discountAmount = originalAmount.multiply(new BigDecimal("0.15"));
            appliedCoupon = "FIRST15";
        }

        BigDecimal finalAmount = originalAmount.subtract(discountAmount);

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid payment method: " + request.getPaymentMethod());
        }

        Rental rental = new Rental();
        rental.setReservationId(request.getReservationId());
        rental.setOriginalAmount(originalAmount);
        rental.setDiscountAmount(discountAmount);
        rental.setFinalAmount(finalAmount);
        rental.setCouponCode(appliedCoupon);
        rental.setPaymentMethod(method);
        rental.setPaymentStatus(PaymentStatus.UNPAID); // initial state

        Rental savedRental = rentalRepository.save(rental);
        return ResponseEntity.ok(savedRental);
    }

    @GetMapping("/reservation/{id}")
    public ResponseEntity<Rental> getRentalByReservationId(@PathVariable("id") Long reservationId) {
        Rental rental = rentalRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException("Rental record not found for reservation #" + reservationId));
        return ResponseEntity.ok(rental);
    }

    @PostMapping("/refund/{reservationId}")
    public ResponseEntity<Void> refundPayment(@PathVariable("reservationId") Long reservationId) {
        Rental rental = rentalRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException("Rental record not found for reservation #" + reservationId));
        
        rental.setPaymentStatus(PaymentStatus.REFUNDED);
        rentalRepository.save(rental);
        return ResponseEntity.ok().build();
    }
}
