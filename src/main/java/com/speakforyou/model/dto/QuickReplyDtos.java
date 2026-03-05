package com.speakforyou.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QuickReplyDtos {

    public record QuickReplyRequest(
            @NotBlank String message,
            @NotNull Long personaId,
            @NotNull Long sceneId
    ) {}
}
