package com.aliren.houserent.subscribe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅（subscribe 表）。
 * type：1=找房源（租客视角） 2=找租客（房东视角）；
 * structured_condition / quiet_hours 为 JSON 列，实体以 String 存储。
 */
@Data
@TableName("subscribe")
public class Subscribe {

    public static final int TYPE_FIND_HOUSE = 1;   // 找房源
    public static final int TYPE_FIND_TENANT = 2;  // 找租客

    public static final int STATUS_ACTIVE = 0;  // 活跃
    public static final int STATUS_PAUSED = 1;  // 暂停
    public static final int STATUS_DELETED = 2; // 已删除（软删）

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private String structuredCondition;
    private String rawText;
    private Integer status;
    private String quietHours;
    private Integer pushCount;
    private LocalDateTime createdAt;
}
