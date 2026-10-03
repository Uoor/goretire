package com.aliren.core.auth;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generateAndParse_roundTrip() {
        JwtUtil jwt = new JwtUtil("aliren-test-secret-key-0123456789abcdef0123456789", 168);
        String token = jwt.generate(42L, "ding-123", 0);
        LoginUser user = jwt.parse(token);
        assertThat(user.getUserId()).isEqualTo(42L);
        assertThat(user.getDingtalkUserId()).isEqualTo("ding-123");
        assertThat(user.getRole()).isZero();
    }

    @Test
    void parse_invalidToken_returnsNull() {
        JwtUtil jwt = new JwtUtil("aliren-test-secret-key-0123456789abcdef0123456789", 168);
        assertThat(jwt.parse("invalid.token.here")).isNull();
    }
}
