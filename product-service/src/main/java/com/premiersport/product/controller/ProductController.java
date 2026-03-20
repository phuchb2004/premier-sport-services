package com.premiersport.product.controller;

import com.premiersport.common.dto.ApiResponse;
import com.premiersport.product.dto.CreateProductRequest;
import com.premiersport.product.dto.UpdateProductRequest;
import com.premiersport.product.entity.ProductEntity;
import com.premiersport.product.service.ProductService;
import com.premiersport.product.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductEntity>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        Page<ProductEntity> products = productService.getProducts(category, search, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductEntity>>> getFeaturedProducts() {
        List<ProductEntity> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryCounts() {
        List<Map<String, Object>> counts = productService.getCategoryCounts();
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<ProductEntity>> getProductById(@PathVariable String id) {
        ProductEntity product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductEntity>> getProductBySlug(@PathVariable String slug) {
        ProductEntity product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/admin/presigned-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam String filename) {
        Map<String, String> result = s3Service.generatePresignedPutUrl(filename);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductEntity>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductEntity product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductEntity>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductEntity product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
