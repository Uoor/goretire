package com.aliren.houserent.house.dto;

/**
 * 发布/编辑房源的结果：返回本次操作后的实际审核状态（ONLINE=已上架 / PENDING=待审核），
 * 前端据此展示准确文案；不暴露内部审核开关配置。
 */
public record PublishResult(Long id, Integer auditStatus) {
}
