package com.aliren.recruit.aitable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 招聘记录（对齐钉钉多维表「岗位信息」主表 8tveFG3 的关键列）。
 *
 * @param recordId     记录 ID（多维表 record id，用于拼岗位详情链接）
 * @param title        职位名称
 * @param company      公司
 * @param locations    工作地点（多选，取 name 列表）
 * @param salary       薪资范围
 * @param description  职位描述
 * @param requirements 任职要求
 * @param contacts     联络人 userId 列表
 * @param priority     优先级（急聘等）
 * @param status       岗位状态（发布中等）
 * @param categories   职类（多选）
 * @param tags         热门标签
 * @param createdAt    创建日期
 */
public record RecruitRecord(
        String recordId,
        String title,
        String company,
        List<String> locations,
        String salary,
        String description,
        String requirements,
        List<String> contacts,
        String priority,
        String status,
        List<String> categories,
        String tags,
        OffsetDateTime createdAt
) {

    /** 是否处于"发布中"（可对外展示）。 */
    public boolean published() {
        return status == null || status.isBlank() || "发布中".equals(status);
    }
}
