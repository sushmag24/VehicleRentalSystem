package com.vehiclerental.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookVehicleRequest {
    private Long customerId;
    private Long vehicleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentMethod; // We'll map this to enum in service
    private String couponCode;
}
