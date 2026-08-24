package com.vehiclerental.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleDTO {
    private Long id;
    private String name;
    private String category;
    private String type;
    private BigDecimal pricePerDay;
    private BigDecimal rating;
    private String imageUrl;
    private String status;
}
