package com.premiersport.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchSuggestionsResponse {
    private List<ProductSuggestionDto> products;
    private List<String> categories;
    private List<String> brands;
}
