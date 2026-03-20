package com.premiersport.chatbot.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolDefinition {

    private String type;
    private FunctionDefinition function;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}
