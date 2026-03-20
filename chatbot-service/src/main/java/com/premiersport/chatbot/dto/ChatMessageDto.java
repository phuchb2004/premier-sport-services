package com.premiersport.chatbot.dto;

import java.time.Instant;

public record ChatMessageDto(String role, String content, Instant timestamp) {
}
