package com.aliren.recruit.aitable;

import java.util.List;

/**
 * 招聘多维表查询抽象。
 * 实现：AitableDingTalkClient（配 aliren.recruit.aitable 凭证后启用，调多维表开放 API）；
 * AitableClientStub（缺省，日志模拟）。
 */
public interface AitableClient {

    /**
     * 按关键词实时查询招聘岗位（仅"发布中"记录）。
     *
     * @param keyword 用户 @ 时输入的内容；空或空白时返回最近一批岗位
     * @return 匹配到的岗位；无匹配返回空列表
     */
    List<RecruitRecord> query(String keyword);
}
