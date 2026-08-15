-- ============================================================
-- 校友安居（阿里/蚂蚁校友租房社群 H5 产品）数据库建表脚本
-- Task 2 of 9：src/main/resources/db/schema.sql
-- 数据库：aliren ｜ MySQL 8.x ｜ InnoDB ｜ utf8mb4
-- 说明：表字段使用 snake_case，与 MyBatis-Plus 默认
--       map-underscore-to-camel-case 映射约定保持一致。
-- ============================================================

CREATE DATABASE IF NOT EXISTS aliren DEFAULT CHARACTER SET utf8mb4;
USE aliren;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dingtalk_userid`  VARCHAR(64)  NOT NULL COMMENT '钉钉用户ID',
  `nickname`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  `role`             TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0=校友 1=管理员',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dingtalk_userid` (`dingtalk_userid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 房源表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `house` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `publisher_id`    BIGINT        NOT NULL COMMENT '发布人ID（user.id）',
  `community`       VARCHAR(128)  NOT NULL COMMENT '小区名称',
  `room_no`         VARCHAR(32)   NOT NULL DEFAULT '' COMMENT '房间号（仅审核可见）',
  `region`          VARCHAR(32)   NOT NULL COMMENT '区域',
  `house_type`      VARCHAR(32)   NOT NULL COMMENT '户型',
  `area`            INT           NOT NULL COMMENT '面积（平方米）',
  `rent`            DECIMAL(10,2) NOT NULL COMMENT '租金（元/月）',
  `deposit_pay`     VARCHAR(16)   NOT NULL COMMENT '押付方式',
  `label`           TINYINT       NOT NULL COMMENT '房源标签：1=房东直租 2=校友转租 3=合租拼室友',
  `pet_ok`          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否允许宠物',
  `commute`         VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '通勤说明',
  `images`          JSON          NULL COMMENT '房源图片（JSON数组）',
  `description`     TEXT          NULL COMMENT '房源描述',
  `audit_status`    TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核 1=已上架 2=已驳回',
  `audit_reason`    VARCHAR(255)  NULL COMMENT '驳回原因',
  `auditor_id`      BIGINT        NULL COMMENT '审核人ID（user.id）',
  `audit_time`      DATETIME      NULL COMMENT '审核时间',
  `rack_status`     TINYINT       NOT NULL DEFAULT 0 COMMENT '上架状态：0=在租中 1=已租出 2=已下架',
  `feedback_answer` TINYINT       NOT NULL DEFAULT 0 COMMENT '反馈回答标记',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_list` (`region`, `audit_status`, `rack_status`),
  KEY `idx_publisher` (`publisher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源表';

-- ------------------------------------------------------------
-- 3. 求租需求表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `demand` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `publisher_id`  BIGINT       NOT NULL COMMENT '发布人ID（user.id）',
  `budget`        JSON         NULL COMMENT '预算（JSON）',
  `region`        VARCHAR(32)  NOT NULL COMMENT '期望区域',
  `house_type`    VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '期望户型',
  `move_in_date`  DATE         NULL COMMENT '期望入住日期',
  `lease_term`    VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '期望租期',
  `requirements`  JSON         NULL COMMENT '其他需求（JSON）',
  `description`   VARCHAR(500) NOT NULL DEFAULT '' COMMENT '需求描述',
  `match_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '匹配状态：0=待匹配 1=已匹配 2=已成交',
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求租需求表';

-- ------------------------------------------------------------
-- 4. 订阅表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `subscribe` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`              BIGINT       NOT NULL COMMENT '用户ID（user.id）',
  `type`                 TINYINT      NOT NULL COMMENT '订阅类型：1=找房源 2=找租客',
  `structured_condition` JSON         NULL COMMENT '结构化订阅条件（JSON）',
  `raw_text`             VARCHAR(500) NOT NULL COMMENT '原始订阅文本',
  `status`               TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=活跃 1=暂停 2=删除',
  `quiet_hours`          JSON         NULL COMMENT '免打扰时段（JSON）',
  `push_count`           INT          NOT NULL DEFAULT 0 COMMENT '累计推送次数',
  `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅表';

-- ------------------------------------------------------------
-- 5. 举报表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `report` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_type` TINYINT      NOT NULL COMMENT '举报对象类型：1=房源 2=用户',
  `target_id`   BIGINT       NOT NULL COMMENT '举报对象ID',
  `reporter_id` BIGINT       NOT NULL COMMENT '举报人ID（user.id）',
  `reason`      VARCHAR(500) NOT NULL COMMENT '举报原因',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0=待处理 1=已处理',
  `result`      VARCHAR(500) NULL COMMENT '处理结果',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- ------------------------------------------------------------
-- 6. 审核日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id` BIGINT      NOT NULL COMMENT '操作人ID（user.id）',
  `action`      VARCHAR(32) NOT NULL COMMENT '操作动作',
  `target_type` VARCHAR(32) NOT NULL COMMENT '操作对象类型',
  `target_id`   BIGINT      NOT NULL COMMENT '操作对象ID',
  `detail`      JSON        NULL COMMENT '操作详情（JSON）',
  `created_at`  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核日志表';
