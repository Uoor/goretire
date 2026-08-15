package com.aliren.auth;

public interface DingTalkClient {
    String getUserIdByCode(String authCode);
}
