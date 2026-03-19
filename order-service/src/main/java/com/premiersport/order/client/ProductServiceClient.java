package com.premiersport.order.client;

import com.premiersport.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${products.service.url:http://localhost:8082}")
    private String productServiceUrl;

    /**
     * Fetches the canonical price (salePrice if set, otherwise price) from product-service.
     * Throws ApiException.badRequest if the product cannot be found or the call fails.
     */
    @SuppressWarnings("unchecked")
    public double getProductPrice(String productId) {
        try {
            String url = productServiceUrl + "/api/v1/products/id/" + productId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
                if (data != null) {
                    Number salePrice = (Number) data.get("salePrice");
                    Number price = (Number) data.get("price");
                    if (salePrice != null) return salePrice.doubleValue();
                    if (price != null) return price.doubleValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch product price for id={}: {}", productId, e.getMessage());
        }
        throw ApiException.badRequest("Unable to verify product price for id: " + productId);
    }
}
