package com.aliren.core.auth;

import lombok.Data;

/**
 * 钉钉用户资料（免登后拉取真实昵称/头像用）。
 * 来自 topapi/v2/user/get 的 result 字段。
 */
@Data
public class DingTalkUserProfile {
    /** 钉钉 userid（与 user.dingtalk_userid 对应） */
    private String userId;
    /** 真实姓名（钉钉通讯录 name） */
    private String name;
    /** 头像 URL（可能为空，钉钉未设置头像时返回空串） */
    private String avatar;
    /** 手机号（可能为空，需通讯录手机号权限且成员未隐藏） */
    private String mobile;
}
