package com.aliren.auth;

import com.aliren.auth.dto.AuthResponse;
import com.aliren.common.BusinessException;
import com.aliren.user.User;
import com.aliren.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final DingTalkClient dingTalkClient;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, DingTalkClient dingTalkClient, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.dingTalkClient = dingTalkClient;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse authenticate(String authCode) {
        String dingtalkUserId;
        try {
            dingtalkUserId = dingTalkClient.getUserIdByCode(authCode);
        } catch (Exception e) {
            throw new BusinessException(401, "免登失败");
        }
        User user = userMapper.selectByDingtalkUserId(dingtalkUserId);
        if (user == null) {
            user = new User();
            user.setDingtalkUserId(dingtalkUserId);
            user.setNickname("校友");
            user.setRole(0);
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已停用");
        }
        String token = jwtUtil.generate(user.getId(), user.getDingtalkUserId(), user.getRole());
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setUserId(user.getId());
        info.setNickname(user.getNickname());
        info.setRole(user.getRole());
        return new AuthResponse(token, info);
    }
}
