package com.aliren.auth;

import com.aliren.auth.dto.AuthResponse;
import com.aliren.common.BusinessException;
import com.aliren.user.User;
import com.aliren.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private DingTalkClient dingTalkClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, dingTalkClient, new JwtUtil(
                "aliren-test-secret-key-0123456789abcdef0123456789", 168));
    }

    @Test
    void auth_newUser_createsAndReturnsToken() {
        when(dingTalkClient.getUserIdByCode("code-1")).thenReturn("ding-100");
        when(userMapper.selectByDingtalkUserId("ding-100")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(1L);
            return 1;
        });

        AuthResponse resp = authService.authenticate("code-1");
        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getUser().getUserId()).isEqualTo(1L);
        assertThat(resp.getUser().getRole()).isZero();
    }

    @Test
    void auth_invalidCode_throws() {
        when(dingTalkClient.getUserIdByCode("bad")).thenThrow(new BusinessException(401, "免登失败"));
        assertThatThrownBy(() -> authService.authenticate("bad"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("免登失败");
    }
}
