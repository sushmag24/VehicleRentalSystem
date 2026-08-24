package com.vehiclerental.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Represents a vehicle available for rent.
 * Status transitions: AVAILABLE → RESERVED → RENTED → AVAILABLE
 *                                          → MAINTENANCE → AVAILABLE
 */
@Entity
@Table(name = "vehicles",
    indexes = {
        @Index(name = "idx_vehicle_status", columnList = "status"),
        @Index(name = "idx_vehicle_type",   columnList = "type")
    }
)
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name of the vehicle (e.g., "Toyota Camry") */
    @Column(nullable = false)
    private String name;

    /** Category of vehicle (TWO_WHEELER or FOUR_WHEELER) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCategory category;

    /** Category of vehicle (e.g., Sedan, SUV, Bike, Scooter) */
    @Column(nullable = false)
    private String type;

    /** Rental cost per day in INR */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    /** Vehicle rating (0.0 to 5.0) */
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    /** URL for vehicle image */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** Current operational status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;
}
