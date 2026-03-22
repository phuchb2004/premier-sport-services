package com.premiersport.product.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class ProductEntity {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String slug;

    private String brand;

    private String description;

    private ProductCategory category;

    private double price;

    private Double salePrice;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private List<String> sizes = new ArrayList<>();

    private int stock;

    @Builder.Default
    @JsonProperty("isFeatured")
    private boolean isFeatured = false;

    @Builder.Default
    private boolean isDeleted = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ProductCategory {
        JERSEY, BOOTS, ACCESSORIES, BALLS
    }
}
