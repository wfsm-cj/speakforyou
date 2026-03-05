package com.speakforyou.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record UserView(
            @NotNull Long id,
            String username,
            String nickname,
            Long defaultPersonaId,
            String modelName,
            boolean hasApiKey
    ) {}

    public record LoginResponse(String token, UserView user) {}
}
