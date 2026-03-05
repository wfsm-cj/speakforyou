package com.speakforyou.controller;

import com.speakforyou.common.ApiResponse;
import com.speakforyou.model.dto.SceneDtos;
import com.speakforyou.service.SceneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenes")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @GetMapping
    public ApiResponse<List<SceneDtos.SceneView>> list() {
        return ApiResponse.ok(sceneService.list());
    }
}
