package com.speakforyou.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserDtos {

    public record UpdateProfileRequest(@NotBlank String nickname, @NotNull Long defaultPersonaId) {}

    public record UpdateApiKeyRequest(@NotBlank String apiKey, @NotBlank String modelName) {}

    public record UsageResponse(String date, int used, int limit, boolean unlimited) {}
}
