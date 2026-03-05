package com.speakforyou.controller;

import com.speakforyou.common.ApiResponse;
import com.speakforyou.model.dto.ChatDtos;
import com.speakforyou.service.ChatService;
import com.speakforyou.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final AuthUtils authUtils;

    public ChatController(ChatService chatService, AuthUtils authUtils) {
        this.chatService = chatService;
        this.authUtils = authUtils;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatDtos.ChatSessionView>> sessions(HttpServletRequest request) {
        return ApiResponse.ok(chatService.listSessions(authUtils.currentUserId(request)));
    }

    @PostMapping("/sessions")
    public ApiResponse<ChatDtos.ChatSessionView> create(
            HttpServletRequest request,
            @RequestBody @Valid ChatDtos.CreateSessionRequest body
    ) {
        return ApiResponse.ok(chatService.createSession(authUtils.currentUserId(request), body));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Map<String, Boolean>> delete(HttpServletRequest request, @PathVariable Long id) {
        chatService.deleteSession(authUtils.currentUserId(request), id);
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatDtos.ChatMessageView>> messages(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(chatService.listMessages(authUtils.currentUserId(request), id));
    }
}
