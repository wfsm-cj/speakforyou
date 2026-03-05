package com.speakforyou.ws;

import com.speakforyou.model.dto.ChatDtos;
import com.speakforyou.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatStompController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void chat(@Valid ChatDtos.WsChatRequest request, Principal principal) {
        String principalName = principal.getName();
        Long userId = Long.parseLong(principalName);
        String destination = "/queue/chat/" + request.sessionId();
        try {
            chatService.streamChat(userId, request.sessionId(), request.message(), new ChatService.ChatPushCallback() {
                @Override
                public void onToken(String text) {
                    messagingTemplate.convertAndSendToUser(principalName, destination, Map.of("type", "token", "data", text));
                }

                @Override
                public void onComplete(String fullText) {
                    messagingTemplate.convertAndSendToUser(principalName, destination, Map.of("type", "complete", "data", fullText));
                }

                @Override
                public void onError(String message) {
                    messagingTemplate.convertAndSendToUser(principalName, destination, Map.of("type", "error", "data", message));
                }
            });
        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(principalName, destination, Map.of(
                    "type", "error",
                    "data", e.getMessage() == null ? "系统错误" : e.getMessage()
            ));
        }
    }
}
