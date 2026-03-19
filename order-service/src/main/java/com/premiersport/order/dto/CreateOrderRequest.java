package com.premiersport.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @Valid
    @NotNull
    private ShippingAddressDto shippingAddress;

    @Data
    public static class ShippingAddressDto {
        @NotBlank
        private String street;

        @NotBlank
        private String city;

        @NotBlank
        private String state;

        @NotBlank
        private String postalCode;

        @NotBlank
        private String country;
    }
}
