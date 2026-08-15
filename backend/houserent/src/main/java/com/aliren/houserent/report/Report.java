package com.aliren.houserent.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报（report 表）。
 * target_type：1=房源 2=用户；status：0=待处理 1=已处理。
 */
@Data
@TableName("report")
public class Report {

    public static final int TYPE_HOUSE = 1;
    public static final int TYPE_USER = 2;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_HANDLED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer targetType;
    private Long targetId;
    private Long reporterId;
    private String reason;
    private Integer status;
    private String result;
    private LocalDateTime createdAt;
}
