package com.aliren.houserent.house.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 联系房东响应：返回房东的钉钉身份（staffId），前端据此唤起钉钉单聊。
 */
@Data
@AllArgsConstructor
public class ContactResponse {
    /** 房东昵称（展示用） */
    private String nickname;
    /** 房东钉钉 userid（staffId），用于 dd.biz.chat.openSingleChat */
    private String staffId;
}
