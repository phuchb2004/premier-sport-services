package com.premiersport.chatbot.dto;

import java.time.Instant;
import java.util.List;

public record ChatHistoryResponse(String sessionId, List<ChatMessageDto> messages, Instant createdAt) {
}
