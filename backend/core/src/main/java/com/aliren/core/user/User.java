package com.aliren.core.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    /** 角色常量：0=普通用户 1=管理员 */
    public static final int ROLE_USER = 0;
    public static final int ROLE_ADMIN = 1;

    /** 状态常量：0=停用 1=正常 */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ACTIVE = 1;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 表列名为 dingtalk_userid（非标准 snake_case），显式标注避免被映射为 dingtalk_user_id */
    @TableField("dingtalk_userid")
    private String dingtalkUserId;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
