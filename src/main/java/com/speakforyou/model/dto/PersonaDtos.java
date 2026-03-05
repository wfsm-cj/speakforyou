package com.speakforyou.model.dto;

import jakarta.validation.constraints.NotBlank;

public class PersonaDtos {

    public record PersonaView(
            Long id,
            String name,
            String description,
            String tone,
            boolean isSystem
    ) {}

    public record CreatePersonaRequest(@NotBlank String name, @NotBlank String description, @NotBlank String tone) {}
}
