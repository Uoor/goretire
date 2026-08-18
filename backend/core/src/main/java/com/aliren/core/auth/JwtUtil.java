package com.aliren.core.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    /** 开发默认密钥（application.yml 内置值）：生产未覆盖时启动告警 */
    private static final String DEFAULT_SECRET_MARKER = "please-change-in-prod";

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${aliren.jwt.secret}") String secret,
                   @Value("${aliren.jwt.expire-hours}") long expireHours) {
        if (secret.contains(DEFAULT_SECRET_MARKER)) {
            log.warn("!!! JWT 正在使用内置开发密钥，生产环境务必通过 JWT_SECRET 环境变量覆盖 !!!");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generate(Long userId, String dingtalkUserId, int role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("dingUserId", dingtalkUserId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            LoginUser u = new LoginUser();
            u.setUserId(Long.valueOf(claims.getSubject()));
            u.setDingtalkUserId(claims.get("dingUserId", String.class));
            u.setRole(claims.get("role", Integer.class));
            return u;
        } catch (Exception e) {
            log.debug("jwt parse failed: {}", e.getMessage());
            return null;
        }
    }
}
