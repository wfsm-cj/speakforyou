package com.speakforyou.service;

import com.speakforyou.common.BizException;
import com.speakforyou.model.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class RateLimitService {

    private final int dailyLimit;
    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public RateLimitService(
            @Value("${app.rate-limit.max-requests-per-day:30}") int dailyLimit,
            StringRedisTemplate redisTemplate
    ) {
        this.dailyLimit = dailyLimit;
        this.redisTemplate = redisTemplate;
    }

    public void checkAndIncrement(UserEntity user) {
        if (user.getApiKey() != null && !user.getApiKey().isBlank()) {
            return;
        }
        String key = keyOf(user.getId());
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, secondsUntilEndOfDay());
        }
        long now = current == null ? 0 : current;
        if (now > dailyLimit) {
            throw new BizException(4290, "今日额度已用完");
        }
    }

    public int usedCount(Long userId) {
        String value = redisTemplate.opsForValue().get(keyOf(userId));
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    private String keyOf(Long userId) {
        return "rate_limit:" + userId + ":" + DATE_FMT.format(LocalDateTime.now());
    }

    private Duration secondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
        return Duration.between(now, end).plusSeconds(1);
    }
}
