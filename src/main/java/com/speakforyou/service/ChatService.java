package com.speakforyou.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakforyou.mapper.ChatMessageMapper;
import com.speakforyou.mapper.ChatSessionMapper;
import com.speakforyou.common.BizException;
import com.speakforyou.model.dto.ChatDtos;
import com.speakforyou.model.entity.ChatMessageEntity;
import com.speakforyou.model.entity.ChatSessionEntity;
import com.speakforyou.model.entity.PersonaEntity;
import com.speakforyou.model.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserService userService;
    private final PersonaService personaService;
    private final RateLimitService rateLimitService;
    private final AiService aiService;

    public ChatService(
            ChatSessionMapper chatSessionMapper,
            ChatMessageMapper chatMessageMapper,
            UserService userService,
            PersonaService personaService,
            RateLimitService rateLimitService,
            AiService aiService
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userService = userService;
        this.personaService = personaService;
        this.rateLimitService = rateLimitService;
        this.aiService = aiService;
    }

    public List<ChatDtos.ChatSessionView> listSessions(Long userId) {
        return chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getUserId, userId)
                        .orderByDesc(ChatSessionEntity::getUpdatedAt))
                .stream()
                .map(s -> new ChatDtos.ChatSessionView(s.getId(), s.getTitle(), s.getPersonaId(), FMT.format(s.getUpdatedAt())))
                .toList();
    }

    @Transactional
    public ChatDtos.ChatSessionView createSession(Long userId, ChatDtos.CreateSessionRequest request) {
        personaService.getPersonaForUser(userId, request.personaId());
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setUserId(userId);
        entity.setPersonaId(request.personaId());
        entity.setTitle((request.title() == null || request.title().isBlank()) ? "新会话" : request.title());
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(entity);
        return new ChatDtos.ChatSessionView(entity.getId(), entity.getTitle(), entity.getPersonaId(), FMT.format(entity.getUpdatedAt()));
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        ChatSessionEntity session = getSession(userId, sessionId);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessageEntity>().eq(ChatMessageEntity::getSessionId, session.getId()));
        chatSessionMapper.deleteById(session.getId());
    }

    public List<ChatDtos.ChatMessageView> listMessages(Long userId, Long sessionId) {
        getSession(userId, sessionId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .orderByAsc(ChatMessageEntity::getSequence))
                .stream()
                .map(m -> new ChatDtos.ChatMessageView(
                        m.getId(), m.getRole(), m.getContent(), m.getSequence(), FMT.format(m.getCreatedAt())
                ))
                .toList();
    }

    public void streamChat(Long userId, Long sessionId, String message, ChatPushCallback callback) {
        UserEntity user = userService.getById(userId);
        rateLimitService.checkAndIncrement(user);
        ChatSessionEntity session = getSession(userId, sessionId);
        PersonaEntity persona = personaService.getPersonaForUser(userId, session.getPersonaId());

        saveMessage(sessionId, "USER", message);

        aiService.streamChatReply(user, persona, message, new AiService.ChatStreamingCallback() {
            @Override
            public void onPartial(String text) {
                callback.onToken(text);
            }

            @Override
            public void onComplete(String fullText) {
                saveMessage(sessionId, "ASSISTANT", fullText);
                session.setUpdatedAt(LocalDateTime.now());
                chatSessionMapper.updateById(session);
                callback.onComplete(fullText);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error.getMessage() == null ? "AI 服务调用失败" : error.getMessage());
            }
        });
    }

    private ChatSessionEntity getSession(Long userId, Long sessionId) {
        ChatSessionEntity session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSessionEntity>()
                .eq(ChatSessionEntity::getId, sessionId)
                .eq(ChatSessionEntity::getUserId, userId)
                .last("limit 1"));
        if (session == null) {
            throw new BizException(4004, "会话不存在");
        }
        return session;
    }

    @Transactional
    protected void saveMessage(Long sessionId, String role, String content) {
        Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId));
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setSequence((count == null ? 0 : count.intValue()) + 1);
        entity.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(entity);
    }

    public interface ChatPushCallback {
        void onToken(String text);

        void onComplete(String fullText);

        void onError(String message);
    }
}
