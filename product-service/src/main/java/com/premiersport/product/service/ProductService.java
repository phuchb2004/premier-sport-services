package com.premiersport.product.service;

import com.premiersport.common.exception.ApiException;
import com.premiersport.product.dto.CreateProductRequest;
import com.premiersport.product.dto.UpdateProductRequest;
import com.premiersport.product.entity.ProductEntity;
import com.premiersport.product.entity.ProductEntity.ProductCategory;
import com.premiersport.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public Page<ProductEntity> getProducts(String category, String search, Double maxPrice, int page, int size, String sort) {
        int clampedSize = Math.max(1, Math.min(size, 100));
        Sort sorting = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, clampedSize, sorting);

        Criteria criteria = new Criteria();

        if (category != null && !category.isBlank()) {
            try {
                ProductCategory cat = ProductCategory.valueOf(category.toUpperCase());
                criteria = criteria.and("category").is(cat);
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Invalid category: " + category);
            }
        }

        if (search != null && !search.isBlank()) {
            String escaped = Pattern.quote(search.trim());
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("name").regex(escaped, "i"),
                    Criteria.where("brand").regex(escaped, "i"),
                    Criteria.where("description").regex(escaped, "i")
            );
            criteria = criteria.andOperator(searchCriteria);
        }

        if (maxPrice != null) {
            criteria = criteria.and("price").lte(maxPrice);
        }

        Query query = new Query(criteria).with(pageable);
        List<ProductEntity> products = mongoTemplate.find(query, ProductEntity.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ProductEntity.class);

        return PageableExecutionUtils.getPage(products, pageable, () -> total);
    }

    public ProductEntity getProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Product not found: " + slug));
    }

    public ProductEntity getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    public List<ProductEntity> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrue();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCategoryCounts() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("category").count().as("count"),
                Aggregation.project("count").and("_id").as("category")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "products", Map.class);
        return results.getMappedResults().stream()
                .map(m -> Map.<String, Object>of(
                        "category", String.valueOf(m.get("category")),
                        "count", m.get("count")
                ))
                .toList();
    }

    public ProductEntity createProduct(CreateProductRequest request) {
        String slug = generateUniqueSlug(request.getName());

        ProductEntity product = ProductEntity.builder()
                .name(request.getName().trim())
                .slug(slug)
                .brand(request.getBrand().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .images(request.getImages())
                .sizes(request.getSizes())
                .stock(request.getStock())
                .isFeatured(request.isFeatured())
                .build();

        product = productRepository.save(product);
        log.info("Product created: {} (slug={})", product.getName(), product.getSlug());
        return product;
    }

    public ProductEntity updateProduct(String id, UpdateProductRequest request) {
        ProductEntity product = getProductById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
            // Regenerate slug only if name changed and new slug doesn't conflict
            String newSlug = generateSlug(request.getName());
            if (!newSlug.equals(product.getSlug()) && !productRepository.existsBySlug(newSlug)) {
                product.setSlug(newSlug);
            }
        }
        if (request.getBrand() != null) product.setBrand(request.getBrand().trim());
        if (request.getDescription() != null) product.setDescription(request.getDescription().trim());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.isClearSalePrice()) {
            product.setSalePrice(null);
        } else if (request.getSalePrice() != null) {
            product.setSalePrice(request.getSalePrice());
        }
        if (request.getImages() != null) product.setImages(request.getImages());
        if (request.getSizes() != null) product.setSizes(request.getSizes());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getIsFeatured() != null) product.setFeatured(request.getIsFeatured());

        product = productRepository.save(product);
        log.info("Product updated: {}", product.getId());
        return product;
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw ApiException.notFound("Product not found");
        }
        productRepository.deleteById(id);
        log.info("Product deleted: {}", id);
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort) {
            case "price-asc"  -> Sort.by(Sort.Direction.ASC,  "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name-asc"   -> Sort.by(Sort.Direction.ASC,  "name");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String generateUniqueSlug(String name) {
        String base = generateSlug(name);
        String slug = base;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
