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
