package com.aliren.houserent.demand;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 求租需求（demand 表）。
 * budget / requirements 为 JSON 列，实体以 String 存储（与 house.images 一致）。
 */
@Data
@TableName("demand")
public class Demand {

    public static final int STATUS_PENDING = 0;  // 待匹配
    public static final int STATUS_MATCHED = 1;  // 已匹配
    public static final int STATUS_DONE = 2;     // 已成交

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private String budget;
    private String region;
    private String houseType;
    private LocalDate moveInDate;
    private String leaseTerm;
    private String requirements;
    private String description;
    private Integer matchStatus;
    private LocalDateTime createdAt;
}
