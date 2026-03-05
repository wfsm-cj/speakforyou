package com.speakforyou.service;

import com.speakforyou.model.dto.QuickReplyDtos;
import com.speakforyou.model.entity.PersonaEntity;
import com.speakforyou.model.entity.SceneEntity;
import com.speakforyou.model.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class QuickReplyService {

    private final UserService userService;
    private final PersonaService personaService;
    private final SceneService sceneService;
    private final RateLimitService rateLimitService;
    private final AiService aiService;

    public QuickReplyService(
            UserService userService,
            PersonaService personaService,
            SceneService sceneService,
            RateLimitService rateLimitService,
            AiService aiService
    ) {
        this.userService = userService;
        this.personaService = personaService;
        this.sceneService = sceneService;
        this.rateLimitService = rateLimitService;
        this.aiService = aiService;
    }

    public SseEmitter streamReply(Long userId, QuickReplyDtos.QuickReplyRequest request) {
        UserEntity user = userService.getById(userId);
        rateLimitService.checkAndIncrement(user);
        PersonaEntity persona = personaService.getPersonaForUser(userId, request.personaId());
        SceneEntity scene = sceneService.getById(request.sceneId());

        SseEmitter emitter = new SseEmitter(60_000L);
        aiService.streamStructuredQuickReply(user, persona, scene, request.message(), emitter);
        return emitter;
    }
}
