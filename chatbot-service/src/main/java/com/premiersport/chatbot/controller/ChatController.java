package com.premiersport.chatbot.controller;

import com.premiersport.chatbot.dto.ChatHistoryResponse;
import com.premiersport.chatbot.dto.CreateSessionResponse;
import com.premiersport.chatbot.dto.SendMessageRequest;
import com.premiersport.chatbot.dto.SendMessageResponse;
import com.premiersport.chatbot.service.ChatService;
import com.premiersport.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<CreateSessionResponse>> createSession() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session created", chatService.createSession()));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<SendMessageResponse>> sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(sessionId, request.message())));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getHistory(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getHistory(sessionId)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> endSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session ended", null));
    }
}
