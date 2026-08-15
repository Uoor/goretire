package com.aliren.core.auth;

public interface DingTalkClient {
    String getUserIdByCode(String authCode);
}
