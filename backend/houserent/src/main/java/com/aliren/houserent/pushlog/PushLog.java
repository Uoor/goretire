package com.aliren.houserent.pushlog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推送日志（push_log 表）：订阅/求租匹配推送历史。
 * subscribe_id 与 demand_id 至少一个非空。
 */
@Data
@TableName("push_log")
public class PushLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subscribeId;
    private Long demandId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}
