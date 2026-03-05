package com.speakforyou.service;

import com.speakforyou.mapper.UserMapper;
import com.speakforyou.common.BizException;
import com.speakforyou.model.dto.UserDtos;
import com.speakforyou.model.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_MODELS = Set.of("qwen-turbo", "qwen-plus", "qwen-max");

    private final UserMapper userMapper;
    private final RateLimitService rateLimitService;

    public UserService(UserMapper userMapper, RateLimitService rateLimitService) {
        this.userMapper = userMapper;
        this.rateLimitService = rateLimitService;
    }

    public UserEntity getById(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(4003, "用户不存在");
        }
        return user;
    }

    @Transactional
    public void updateProfile(Long userId, UserDtos.UpdateProfileRequest request) {
        UserEntity user = getById(userId);
        user.setNickname(request.nickname());
        user.setDefaultPersonaId(request.defaultPersonaId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updateApiKey(Long userId, UserDtos.UpdateApiKeyRequest request) {
        if (!ALLOWED_MODELS.contains(request.modelName())) {
            throw new BizException(4001, "modelName 不合法");
        }
        UserEntity user = getById(userId);
        user.setApiKey(request.apiKey());
        user.setModelName(request.modelName());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void clearApiKey(Long userId) {
        UserEntity user = getById(userId);
        user.setApiKey(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public UserDtos.UsageResponse usage(Long userId) {
        UserEntity user = getById(userId);
        boolean unlimited = user.getApiKey() != null && !user.getApiKey().isBlank();
        return new UserDtos.UsageResponse(
                LocalDate.now().toString(),
                rateLimitService.usedCount(userId),
                rateLimitService.dailyLimit(),
                unlimited
        );
    }
}
