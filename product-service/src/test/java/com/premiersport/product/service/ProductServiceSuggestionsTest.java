package com.premiersport.product.service;

import com.premiersport.product.dto.SearchSuggestionsResponse;
import com.premiersport.product.entity.ProductEntity;
import com.premiersport.product.entity.ProductEntity.ProductCategory;
import com.premiersport.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceSuggestionsTest {

    @Mock private ProductRepository productRepository;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductService productService;

    // --- input validation ---

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a"})
    void getSuggestions_blankOrTooShort_returnsEmpty(String q) {
        SearchSuggestionsResponse result = productService.getSuggestions(q);

        assertThat(result.getProducts()).isEmpty();
        assertThat(result.getCategories()).isEmpty();
        assertThat(result.getBrands()).isEmpty();
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void getSuggestions_nullQuery_returnsEmpty() {
        SearchSuggestionsResponse result = productService.getSuggestions(null);

        assertThat(result.getProducts()).isEmpty();
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void getSuggestions_queryOver50Chars_returnsEmpty() {
        String longQuery = "a".repeat(51);
        SearchSuggestionsResponse result = productService.getSuggestions(longQuery);

        assertThat(result.getProducts()).isEmpty();
        verifyNoInteractions(mongoTemplate);
    }

    // --- no results ---

    @Test
    void getSuggestions_noMatches_returnsEmptyLists() {
        when(mongoTemplate.find(any(), eq(ProductEntity.class))).thenReturn(List.of());

        SearchSuggestionsResponse result = productService.getSuggestions("nike");

        assertThat(result.getProducts()).isEmpty();
        assertThat(result.getCategories()).isEmpty();
        assertThat(result.getBrands()).isEmpty();
    }

    // --- products capped at 5 ---

    @Test
    void getSuggestions_moreThanFiveMatches_returnsOnlyFiveProducts() {
        List<ProductEntity> eight = buildProducts(8, ProductCategory.BOOTS, "Nike");
        when(mongoTemplate.find(any(), eq(ProductEntity.class))).thenReturn(eight);

        SearchSuggestionsResponse result = productService.getSuggestions("nike");

        assertThat(result.getProducts()).hasSize(5);
    }

    // --- categories and brands from full 20-result set ---

    @Test
    void getSuggestions_multipleCategories_returnsDistinctCategoriesFromAll20() {
        List<ProductEntity> matches = new ArrayList<>();
        matches.addAll(buildProducts(3, ProductCategory.BOOTS, "Nike"));
        matches.addAll(buildProducts(3, ProductCategory.KITS, "Adidas"));
        when(mongoTemplate.find(any(), eq(ProductEntity.class))).thenReturn(matches);

        SearchSuggestionsResponse result = productService.getSuggestions("sport");

        // Products capped at 5, but categories come from all 6
        assertThat(result.getProducts()).hasSize(5);
        assertThat(result.getCategories()).containsExactlyInAnyOrder("BOOTS", "KITS");
        assertThat(result.getBrands()).containsExactlyInAnyOrder("Nike", "Adidas");
    }

    // --- image uses first element ---

    @Test
    void getSuggestions_productWithImages_usesFirstImage() {
        ProductEntity product = buildProduct("id-1", ProductCategory.BOOTS, "Nike");
        product.setImages(List.of("https://img1.jpg", "https://img2.jpg"));
        when(mongoTemplate.find(any(), eq(ProductEntity.class))).thenReturn(List.of(product));

        SearchSuggestionsResponse result = productService.getSuggestions("nike");

        assertThat(result.getProducts().get(0).getImage()).isEqualTo("https://img1.jpg");
    }

    @Test
    void getSuggestions_productWithNoImages_imageIsNull() {
        ProductEntity product = buildProduct("id-1", ProductCategory.BOOTS, "Nike");
        product.setImages(List.of());
        when(mongoTemplate.find(any(), eq(ProductEntity.class))).thenReturn(List.of(product));

        SearchSuggestionsResponse result = productService.getSuggestions("nike");

        assertThat(result.getProducts().get(0).getImage()).isNull();
    }

    // --- helpers ---

    private List<ProductEntity> buildProducts(int count, ProductCategory category, String brand) {
        List<ProductEntity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(buildProduct("id-" + category + "-" + i, category, brand));
        }
        return list;
    }

    private ProductEntity buildProduct(String id, ProductCategory category, String brand) {
        return ProductEntity.builder()
                .id(id)
                .name("Product " + id)
                .slug("product-" + id)
                .brand(brand)
                .category(category)
                .price(49.99)
                .images(new ArrayList<>())
                .build();
    }
}
