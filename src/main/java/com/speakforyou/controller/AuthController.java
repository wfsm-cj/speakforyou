package com.speakforyou.controller;

import com.speakforyou.common.ApiResponse;
import com.speakforyou.model.dto.AuthDtos;
import com.speakforyou.service.AuthService;
import com.speakforyou.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthUtils authUtils;

    public AuthController(AuthService authService, AuthUtils authUtils) {
        this.authService = authService;
        this.authUtils = authUtils;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(@RequestBody @Valid AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Boolean>> logout() {
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserView> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.me(authUtils.currentUserId(request)));
    }
}
