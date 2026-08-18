package com.aliren.core.auth;

import com.aliren.core.auth.dto.AuthResponse;
import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void auth_disabledUser_rejected403() {
        when(dingTalkClient.getUserIdByCode("code-2")).thenReturn("ding-200");
        User existing = new User();
        existing.setId(2L);
        existing.setDingtalkUserId("ding-200");
        existing.setStatus(0);
        when(userMapper.selectByDingtalkUserId("ding-200")).thenReturn(existing);

        assertThatThrownBy(() -> authService.authenticate("code-2"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用");
    }

    @Test
    void auth_existingUser_doesNotInsert() {
        when(dingTalkClient.getUserIdByCode("code-3")).thenReturn("ding-300");
        User existing = new User();
        existing.setId(3L);
        existing.setDingtalkUserId("ding-300");
        existing.setRole(0);
        existing.setStatus(1);
        when(userMapper.selectByDingtalkUserId("ding-300")).thenReturn(existing);

        AuthResponse resp = authService.authenticate("code-3");
        assertThat(resp.getUser().getUserId()).isEqualTo(3L);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void auth_newUser_withProfile_fillsNicknameAndAvatar() {
        when(dingTalkClient.getUserIdByCode("code-5")).thenReturn("ding-500");
        when(userMapper.selectByDingtalkUserId("ding-500")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(5L);
            return 1;
        });
        DingTalkUserProfile profile = new DingTalkUserProfile();
        profile.setUserId("ding-500");
        profile.setName("峰");
        profile.setAvatar("https://example.com/a.png");
        when(dingTalkClient.getUserProfile("ding-500")).thenReturn(profile);

        AuthResponse resp = authService.authenticate("code-5");
        assertThat(resp.getUser().getNickname()).isEqualTo("峰");
        assertThat(resp.getUser().getUserId()).isEqualTo(5L);
    }

    @Test
    void auth_existingUser_withProfile_updatesNickname() {
        when(dingTalkClient.getUserIdByCode("code-6")).thenReturn("ding-600");
        User existing = new User();
        existing.setId(6L);
        existing.setDingtalkUserId("ding-600");
        existing.setNickname("旧昵称");
        existing.setRole(0);
        existing.setStatus(1);
        when(userMapper.selectByDingtalkUserId("ding-600")).thenReturn(existing);
        DingTalkUserProfile profile = new DingTalkUserProfile();
        profile.setUserId("ding-600");
        profile.setName("新昵称");
        when(dingTalkClient.getUserProfile("ding-600")).thenReturn(profile);

        AuthResponse resp = authService.authenticate("code-6");
        assertThat(resp.getUser().getNickname()).isEqualTo("新昵称");
        verify(userMapper).updateById(existing);
    }

    @Test
    void auth_existingUser_profileFails_keepsOldData() {
        when(dingTalkClient.getUserIdByCode("code-7")).thenReturn("ding-700");
        User existing = new User();
        existing.setId(7L);
        existing.setDingtalkUserId("ding-700");
        existing.setNickname("保持");
        existing.setRole(0);
        existing.setStatus(1);
        when(userMapper.selectByDingtalkUserId("ding-700")).thenReturn(existing);
        when(dingTalkClient.getUserProfile("ding-700")).thenReturn(null);

        AuthResponse resp = authService.authenticate("code-7");
        assertThat(resp.getUser().getNickname()).isEqualTo("保持");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void auth_blankUserId_rejected401() {
        when(dingTalkClient.getUserIdByCode("code-4")).thenReturn("");
        assertThatThrownBy(() -> authService.authenticate("code-4"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("免登失败");
    }
}
