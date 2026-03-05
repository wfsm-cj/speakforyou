package com.speakforyou.controller;

import com.speakforyou.common.ApiResponse;
import com.speakforyou.model.dto.UserDtos;
import com.speakforyou.service.UserService;
import com.speakforyou.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AuthUtils authUtils;

    public UserController(UserService userService, AuthUtils authUtils) {
        this.userService = userService;
        this.authUtils = authUtils;
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Boolean>> updateProfile(
            HttpServletRequest request,
            @RequestBody @Valid UserDtos.UpdateProfileRequest body
    ) {
        userService.updateProfile(authUtils.currentUserId(request), body);
        return ApiResponse.ok(Map.of("success", true));
    }

    @PutMapping("/api-key")
    public ApiResponse<Map<String, Boolean>> updateApiKey(
            HttpServletRequest request,
            @RequestBody @Valid UserDtos.UpdateApiKeyRequest body
    ) {
        userService.updateApiKey(authUtils.currentUserId(request), body);
        return ApiResponse.ok(Map.of("success", true));
    }

    @DeleteMapping("/api-key")
    public ApiResponse<Map<String, Boolean>> clearApiKey(HttpServletRequest request) {
        userService.clearApiKey(authUtils.currentUserId(request));
        return ApiResponse.ok(Map.of("success", true));
    }

    @GetMapping("/usage")
    public ApiResponse<UserDtos.UsageResponse> usage(HttpServletRequest request) {
        return ApiResponse.ok(userService.usage(authUtils.currentUserId(request)));
    }
}
