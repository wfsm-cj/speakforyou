package com.speakforyou.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakforyou.mapper.UserMapper;
import com.speakforyou.common.BizException;
import com.speakforyou.model.dto.AuthDtos;
import com.speakforyou.model.entity.UserEntity;
import com.speakforyou.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, request.username())
                .last("limit 1"));
        if (user == null) {
            throw new BizException(4003, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(4003, "用户名或密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthDtos.LoginResponse(token, toUserView(user));
    }

    public AuthDtos.UserView me(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(4003, "用户不存在");
        }
        return toUserView(user);
    }

    public AuthDtos.UserView toUserView(UserEntity user) {
        return new AuthDtos.UserView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getDefaultPersonaId(),
                user.getModelName(),
                user.getApiKey() != null && !user.getApiKey().isBlank()
        );
    }
}
