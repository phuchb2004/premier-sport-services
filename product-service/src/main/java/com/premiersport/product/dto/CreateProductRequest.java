package com.premiersport.product.dto;

import com.premiersport.product.entity.ProductEntity.ProductCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    private ProductCategory category;

    @Positive(message = "Price must be positive")
    private double price;

    private Double salePrice;

    private List<String> images = new ArrayList<>();

    private List<String> sizes = new ArrayList<>();

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;

    private boolean isFeatured = false;
}
