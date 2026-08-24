package com.vehiclerental.repository;

import com.vehiclerental.model.Rental;
import com.vehiclerental.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    Optional<Rental> findByReservationId(Long reservationId);

    /** Count rentals that successfully used a coupon (not refunded/cancelled). */
    long countByCouponCodeAndPaymentStatusNot(String couponCode, PaymentStatus paymentStatus);
}
