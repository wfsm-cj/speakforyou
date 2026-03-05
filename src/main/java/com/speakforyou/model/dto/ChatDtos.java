package com.speakforyou.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatDtos {

    public record CreateSessionRequest(@NotNull Long personaId, String title) {}

    public record ChatSessionView(Long id, String title, Long personaId, String updatedAt) {}

    public record ChatMessageView(Long id, String role, String content, Integer sequence, String createdAt) {}

    public record WsChatRequest(@NotNull Long sessionId, @NotBlank String message) {}
}
