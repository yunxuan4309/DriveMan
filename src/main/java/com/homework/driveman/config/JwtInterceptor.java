package com.homework.driveman.config;

import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.utils.JwtUtils;
import com.homework.driveman.web.ServiceCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 拦截器 — 校验 Token + 角色权限
 * 公开路径不拦截（登录、文档、静态资源），其余接口均需 Token
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 非 HandlerMethod（如静态资源）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 从请求头获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ServiceException(ServiceCode.ERROR_UNAUTHORIZED, "未登录，请先登录");
        }

        String token = authHeader.substring(7);
        CurrentUser currentUser = jwtUtils.parseToken(token);
        if (currentUser == null) {
            throw new ServiceException(ServiceCode.ERROR_UNAUTHORIZED, "登录已过期，请重新登录");
        }

        // 将当前用户信息存入 request 属性
        request.setAttribute("currentUser", currentUser);

        // 检查 @RequireRole 注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            int[] allowedRoles = requireRole.value();
            if (Arrays.stream(allowedRoles).noneMatch(r -> r == currentUser.getRole())) {
                throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "权限不足");
            }
        }

        return true;
    }
}
