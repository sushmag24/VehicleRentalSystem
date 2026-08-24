package com.vehiclerental.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

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

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCategory category;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;
}
