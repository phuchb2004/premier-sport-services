package com.premiersport.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 1000, message = "Message too long")
        String message
) {
}
