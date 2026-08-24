package com.vehiclerental.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Reservation entity - links a customer to a vehicle for a date range.
 * Uses @JsonIgnoreProperties to prevent infinite recursion during JSON serialization
 * (since Vehicle and User have their own relationships).
 */
@Entity
@Table(name = "reservations",
    indexes = {
        @Index(name = "idx_reservation_customer", columnList = "customer_id"),
        @Index(name = "idx_reservation_vehicle",  columnList = "vehicle_id"),
        @Index(name = "idx_reservation_dates",    columnList = "start_date, end_date")
    }
)
@Data
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer who made this reservation */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"reservations", "password", "hibernateLazyInitializer"})
    private User customer;

    /** The vehicle being reserved */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnoreProperties({"reservations", "hibernateLazyInitializer"})
    private Vehicle vehicle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    /** Financial details for this reservation */
    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("reservation")
    private Rental rental;
}
