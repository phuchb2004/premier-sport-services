package com.premiersport.chatbot.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product-service.url:http://localhost:8082}")
    private String productServiceUrl;

    @SuppressWarnings("unchecked")
    public String searchProducts(String query, String category, Double maxPrice) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(productServiceUrl + "/api/v1/products")
                    .queryParam("search", query)
                    .queryParam("size", 5);

            if (category != null && !category.isBlank()) {
                builder.queryParam("category", category);
            }
            if (maxPrice != null) {
                builder.queryParam("maxPrice", maxPrice);
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    builder.toUriString(), Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "No products found.";
            }

            Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
            if (data == null) return "No products found.";

            List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
            if (content == null || content.isEmpty()) {
                return "No products found matching your search.";
            }

            StringBuilder result = new StringBuilder("Found ").append(content.size()).append(" product(s):\n");
            for (Map<String, Object> product : content) {
                String name = (String) product.get("name");
                String slug = (String) product.get("slug");
                String cat = (String) product.get("category");
                String brand = (String) product.get("brand");
                Number price = (Number) product.get("price");
                Number salePrice = (Number) product.get("salePrice");

                double displayPrice = salePrice != null ? salePrice.doubleValue()
                        : (price != null ? price.doubleValue() : 0);
                String catLower = cat != null ? cat.toLowerCase() : "products";

                result.append("- ").append(name)
                        .append(" by ").append(brand)
                        .append(" | £").append(String.format("%.2f", displayPrice))
                        .append(" | Link: /products/").append(catLower).append("/").append(slug)
                        .append("\n");
            }
            return result.toString();

        } catch (Exception e) {
            log.warn("Error searching products: {}", e.getMessage());
            return "Unable to search products at this time.";
        }
    }
}
