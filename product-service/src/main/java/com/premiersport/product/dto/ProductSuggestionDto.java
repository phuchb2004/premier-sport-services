package com.premiersport.product.dto;

import com.premiersport.product.entity.ProductEntity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSuggestionDto {
    private String id;
    private String name;
    private String slug;
    private String image;        // null if product has no images
    private double price;
    private Double salePrice;    // null if no sale
    private ProductCategory category;  // serialises to uppercase enum name e.g. "BOOTS"
}
