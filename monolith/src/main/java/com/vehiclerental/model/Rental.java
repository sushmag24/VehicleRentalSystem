package com.vehiclerental.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Rental record created automatically when a reservation is confirmed.
 * Tracks the financial side: total cost and payment state.
 */
@Entity
@Table(name = "rentals")
@Data
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One reservation maps to exactly one rental record */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Reservation reservation;

    /** Original price without discounts */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    /** Savings from coupons/discounts */
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Total amount to be paid (original - discount) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    /** Coupon code applied, if any */
    @Column(length = 20)
    private String couponCode;

    /** Method chosen by user (GPAY, CARD, etc) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    /** Payment state: UNPAID | PAID | REFUNDED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
}
