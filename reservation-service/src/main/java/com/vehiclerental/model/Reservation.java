package com.vehiclerental.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

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

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;
}
