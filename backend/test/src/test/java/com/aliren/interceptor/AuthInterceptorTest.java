package com.aliren.core.interceptor;

import com.aliren.core.auth.JwtUtil;
import com.aliren.core.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthInterceptorTest {

    private AuthInterceptor interceptor;
    private JwtUtil jwt;

    @BeforeEach
    void setUp() {
        jwt = new JwtUtil("aliren-test-secret-key-0123456789abcdef0123456789", 168);
        interceptor = new AuthInterceptor(jwt);
        UserContext.clear();
    }

    @Test
    void validToken_passAndSetContext() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt.generate(1L, "ding-1", 0));
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean ok = interceptor.preHandle(req, resp, new Object());
        assertThat(ok).isTrue();
        assertThat(UserContext.get().getUserId()).isEqualTo(1L);
    }

    @Test
    void missingToken_reject401() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean ok = interceptor.preHandle(req, resp, new Object());
        assertThat(ok).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    void invalidToken_reject401() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean ok = interceptor.preHandle(req, resp, new Object());
        assertThat(ok).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    void afterCompletion_clearsContext() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt.generate(1L, "ding-1", 0));
        MockHttpServletResponse resp = new MockHttpServletResponse();

        interceptor.preHandle(req, resp, new Object());
        assertThat(UserContext.get()).isNotNull();
        interceptor.afterCompletion(req, resp, new Object(), null);
        assertThat(UserContext.get()).isNull();
    }
}
