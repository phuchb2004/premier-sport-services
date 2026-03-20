package com.premiersport.chatbot.openai;

import com.premiersport.chatbot.openai.dto.ChatCompletionRequest;
import com.premiersport.chatbot.openai.dto.ChatCompletionResponse;
import com.premiersport.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String apiKey;

    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                    OPENAI_URL, entity, ChatCompletionResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw ApiException.badRequest("OpenAI returned non-success status");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            throw ApiException.badRequest("Failed to get AI response. Please try again.");
        }
    }
}
