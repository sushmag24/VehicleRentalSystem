package com.vehiclerental.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long reservationId;
    private Long customerId;
    private BigDecimal originalAmount;
    private String couponCode;
    private String paymentMethod;
    private Long previousBookingsCount;
}
