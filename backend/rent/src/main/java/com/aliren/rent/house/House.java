package com.aliren.rent.house;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("house")
public class House {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private String community;
    private String roomNo;
    private String region;
    private String houseType;
    private Integer area;
    private BigDecimal rent;
    private String depositPay;
    private Integer label;
    private Integer petOk;
    private String commute;
    private String images;
    private String description;
    private Integer auditStatus;
    private String auditReason;
    private Long auditorId;
    private LocalDateTime auditTime;
    private Integer rackStatus;
    private Integer feedbackAnswer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
