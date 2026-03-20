package com.premiersport.chatbot.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiMessage {

    private String role;
    private String content;

    @JsonProperty("tool_calls")
    private List<ToolCallDto> toolCalls;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    public static OpenAiMessage system(String content) {
        return OpenAiMessage.builder().role("system").content(content).build();
    }

    public static OpenAiMessage user(String content) {
        return OpenAiMessage.builder().role("user").content(content).build();
    }

    public static OpenAiMessage assistant(String content) {
        return OpenAiMessage.builder().role("assistant").content(content).build();
    }

    public static OpenAiMessage toolResult(String toolCallId, String content) {
        return OpenAiMessage.builder().role("tool").toolCallId(toolCallId).content(content).build();
    }
}
