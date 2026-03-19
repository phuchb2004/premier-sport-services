package com.premiersport.product.dto;

import com.premiersport.product.entity.ProductEntity.ProductCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProductRequest {

    private String name;

    private String brand;

    private String description;

    private ProductCategory category;

    @Positive(message = "Price must be positive")
    private Double price;

    private Double salePrice;

    private List<String> images;

    private List<String> sizes;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private Boolean isFeatured;
}
