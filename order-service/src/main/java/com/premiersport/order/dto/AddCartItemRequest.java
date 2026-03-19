package com.premiersport.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCartItemRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String productName;

    private String productImage;

    @NotBlank
    private String size;

    private String color;

    @NotNull
    @Min(1)
    private Integer quantity;

    // unitPrice is intentionally NOT accepted from the client.
    // The server fetches the authoritative price from product-service.
}
