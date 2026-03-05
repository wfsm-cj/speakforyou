package com.speakforyou.util;

import com.speakforyou.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    public Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value == null) {
            throw new BizException(4003, "未登录或 token 无效");
        }
        return (Long) value;
    }
}
