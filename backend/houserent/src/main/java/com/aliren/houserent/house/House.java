package com.aliren.houserent.house;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("house")
public class House {

    /** 审核状态：0=待审核 1=已上架 2=已驳回 */
    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_ONLINE = 1;
    public static final int AUDIT_REJECTED = 2;

    /** 上架状态：0=在租中 1=已租出 2=已下架 */
    public static final int RACK_RENTING = 0;
    public static final int RACK_RENTED = 1;
    public static final int RACK_OFF = 2;

    /** 房源标签：1=房东直租 2=校友转租 3=合租拼室友 */
    public static final int LABEL_DIRECT = 1;
    public static final int LABEL_TRANSFER = 2;
    public static final int LABEL_SHARE = 3;

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
    private String leaseTerm;
    private Integer label;
    private Integer petOk;
    private String commute;
    /** 水电网物业说明（如"含水电网"） */
    private String utilities;
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

    /** 金额去尾零：6000.00 → 6000 */
    public String getRentText() {
        return rent == null ? "" : rent.stripTrailingZeros().toPlainString();
    }
}
