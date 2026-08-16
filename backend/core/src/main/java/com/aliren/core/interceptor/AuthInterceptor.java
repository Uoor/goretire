package com.aliren.core.interceptor;

import com.aliren.core.auth.JwtUtil;
import com.aliren.core.auth.LoginUser;
import com.aliren.core.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器：解析 Authorization: Bearer <token>，
 * 有效则写入 UserContext 放行，无效返回 401。
 * 注意：此拦截器只做认证，不做角色校验（管理员校验在业务层/专用接口中）。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            LoginUser user = jwtUtil.parse(auth.substring(7));
            if (user != null) {
                UserContext.set(user);
                log.info("[auth] PASS {} (userId={})", request.getRequestURI(), user.getUserId());
                return true;
            }
        }
        log.info("[auth] 401 {} (token={})", request.getRequestURI(),
                auth != null ? auth.substring(0, Math.min(20, auth.length())) + "..." : "none");
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
