package com.premiersport.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premiersport.chatbot.dto.ChatHistoryResponse;
import com.premiersport.chatbot.dto.ChatMessageDto;
import com.premiersport.chatbot.dto.CreateSessionResponse;
import com.premiersport.chatbot.dto.SendMessageResponse;
import com.premiersport.chatbot.entity.ChatMessageDocument;
import com.premiersport.chatbot.entity.ChatSessionEntity;
import com.premiersport.chatbot.openai.OpenAiClient;
import com.premiersport.chatbot.openai.dto.ChatCompletionRequest;
import com.premiersport.chatbot.openai.dto.ChatCompletionResponse;
import com.premiersport.chatbot.openai.dto.OpenAiMessage;
import com.premiersport.chatbot.openai.dto.SearchProductsArgs;
import com.premiersport.chatbot.openai.dto.ToolCallDto;
import com.premiersport.chatbot.openai.dto.ToolDefinition;
import com.premiersport.chatbot.product.ProductServiceClient;
import com.premiersport.chatbot.repository.ChatSessionRepository;
import com.premiersport.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final OpenAiClient openAiClient;
    private final ProductServiceClient productServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            You are a helpful Premier Sport assistant specialised in football and sports gear.
            You help customers find the right products: jerseys, boots, accessories, and balls.
            When a customer asks about specific products, always use the searchProducts function to find relevant items from our catalogue.
            Always include the product link (e.g. /products/boots/nike-phantom-gt) so customers can view and buy.
            Be friendly, concise, and helpful. Keep responses under 200 words.
            Format product recommendations as a short list with name, price, and link.
            If asked about non-sports topics, politely redirect to sports gear.
            """;

    public CreateSessionResponse createSession() {
        ChatSessionEntity session = ChatSessionEntity.builder()
                .messages(new ArrayList<>())
                .build();
        session = sessionRepository.save(session);
        return new CreateSessionResponse(session.getId());
    }

    public SendMessageResponse sendMessage(String sessionId, String userMessage) {
        ChatSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Chat session not found: " + sessionId));

        session.getMessages().add(ChatMessageDocument.builder()
                .role("user")
                .content(userMessage)
                .timestamp(Instant.now())
                .build());

        List<OpenAiMessage> openAiMessages = buildOpenAiMessages(session.getMessages());

        String botResponse = callOpenAiWithFunctionLoop(openAiMessages);

        session.getMessages().add(ChatMessageDocument.builder()
                .role("assistant")
                .content(botResponse)
                .timestamp(Instant.now())
                .build());

        sessionRepository.save(session);
        return new SendMessageResponse(sessionId, botResponse);
    }

    public ChatHistoryResponse getHistory(String sessionId) {
        ChatSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Chat session not found: " + sessionId));

        List<ChatMessageDto> messages = session.getMessages().stream()
                .map(m -> new ChatMessageDto(m.getRole(), m.getContent(), m.getTimestamp()))
                .toList();

        return new ChatHistoryResponse(sessionId, messages, session.getCreatedAt());
    }

    public void deleteSession(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw ApiException.notFound("Chat session not found: " + sessionId);
        }
        sessionRepository.deleteById(sessionId);
    }

    private List<OpenAiMessage> buildOpenAiMessages(List<ChatMessageDocument> history) {
        List<OpenAiMessage> messages = new ArrayList<>();
        messages.add(OpenAiMessage.system(SYSTEM_PROMPT));
        for (ChatMessageDocument msg : history) {
            messages.add(OpenAiMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build());
        }
        return messages;
    }

    private String callOpenAiWithFunctionLoop(List<OpenAiMessage> messages) {
        List<ToolDefinition> tools = List.of(buildSearchProductsTool());

        // First call — with tools
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(new ArrayList<>(messages))
                .tools(tools)
                .toolChoice("auto")
                .build();

        ChatCompletionResponse response = openAiClient.chatCompletion(request);
        ChatCompletionResponse.Choice choice = response.getChoices().get(0);

        if ("tool_calls".equals(choice.getFinishReason()) && choice.getMessage().getToolCalls() != null) {
            // Add assistant message with tool_calls to context
            messages.add(choice.getMessage());

            // Execute each tool call and add results
            for (ToolCallDto toolCall : choice.getMessage().getToolCalls()) {
                String toolResult = executeToolCall(toolCall);
                log.debug("Tool call {} result: {}", toolCall.getFunction().getName(), toolResult);
                messages.add(OpenAiMessage.toolResult(toolCall.getId(), toolResult));
            }

            // Second call — without tools to get final text response
            ChatCompletionRequest followUp = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .build();

            ChatCompletionResponse finalResponse = openAiClient.chatCompletion(followUp);
            String content = finalResponse.getChoices().get(0).getMessage().getContent();
            return content != null ? content : "Here are some products I found for you!";
        }

        // Direct text response (no tool call needed)
        String content = choice.getMessage().getContent();
        return content != null ? content : "I'm here to help! How can I assist?";
    }

    private String executeToolCall(ToolCallDto toolCall) {
        if (!"searchProducts".equals(toolCall.getFunction().getName())) {
            return "Unknown function: " + toolCall.getFunction().getName();
        }
        try {
            SearchProductsArgs args = objectMapper.readValue(
                    toolCall.getFunction().getArguments(), SearchProductsArgs.class);
            return productServiceClient.searchProducts(args.getQuery(), args.getCategory(), args.getMaxPrice());
        } catch (Exception e) {
            log.warn("Error executing searchProducts: {}", e.getMessage());
            return "Could not search products at this time.";
        }
    }

    private ToolDefinition buildSearchProductsTool() {
        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "Search query (e.g. 'Nike boots', 'football kit')");

        Map<String, Object> categoryProp = new LinkedHashMap<>();
        categoryProp.put("type", "string");
        categoryProp.put("enum", List.of("JERSEY", "BOOTS", "ACCESSORIES", "BALLS"));
        categoryProp.put("description", "Filter by product category");

        Map<String, Object> maxPriceProp = new LinkedHashMap<>();
        maxPriceProp.put("type", "number");
        maxPriceProp.put("description", "Maximum price in GBP");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", queryProp);
        properties.put("category", categoryProp);
        properties.put("maxPrice", maxPriceProp);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("query"));

        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("searchProducts")
                        .description("Search for sports products in the Premier Sport catalogue. Use when a customer asks about products, categories, brands, or prices.")
                        .parameters(parameters)
                        .build())
                .build();
    }
}
