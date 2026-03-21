package com.premiersport.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {

    @NotBlank
    private String paymentMethod; // "VIETQR" or "MOMO"
}
