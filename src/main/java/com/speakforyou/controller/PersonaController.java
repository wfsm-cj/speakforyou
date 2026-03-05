package com.speakforyou.controller;

import com.speakforyou.common.ApiResponse;
import com.speakforyou.model.dto.PersonaDtos;
import com.speakforyou.service.PersonaService;
import com.speakforyou.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;
    private final AuthUtils authUtils;

    public PersonaController(PersonaService personaService, AuthUtils authUtils) {
        this.personaService = personaService;
        this.authUtils = authUtils;
    }

    @GetMapping
    public ApiResponse<List<PersonaDtos.PersonaView>> list(HttpServletRequest request) {
        return ApiResponse.ok(personaService.list(authUtils.currentUserId(request)));
    }

    @PostMapping
    public ApiResponse<PersonaDtos.PersonaView> create(
            HttpServletRequest request,
            @RequestBody @Valid PersonaDtos.CreatePersonaRequest body
    ) {
        return ApiResponse.ok(personaService.create(authUtils.currentUserId(request), body));
    }

    @PutMapping("/{id}")
    public ApiResponse<PersonaDtos.PersonaView> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody @Valid PersonaDtos.CreatePersonaRequest body
    ) {
        return ApiResponse.ok(personaService.update(authUtils.currentUserId(request), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(HttpServletRequest request, @PathVariable Long id) {
        personaService.delete(authUtils.currentUserId(request), id);
        return ApiResponse.ok(Map.of("success", true));
    }
}
