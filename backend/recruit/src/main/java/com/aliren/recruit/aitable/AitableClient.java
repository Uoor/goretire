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

    /**
     * 查询最近 N 天内新增的岗位（仅"发布中"，按创建时间最新在前）。
     * 用于空 @ 时的"过去一周总结"。
     *
     * @param days 天数，如 7 表示最近 7 天
     * @return 最近新增岗位；无则返回空列表
     */
    List<RecruitRecord> queryRecent(int days);

    /**
     * 查询按创建时间最新在前的 N 条岗位（仅"发布中"）。
     * 用于"过去一周无新增"时的回退展示。
     *
     * @param limit 条数上限
     * @return 最新岗位；无则返回空列表
     */
    List<RecruitRecord> queryLatest(int limit);
}
