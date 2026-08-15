package com.aliren.houserent.match.dto;

import lombok.Data;

import java.util.List;

/** 一句话找房响应 */
@Data
public class MatchSearchResponse {
    /** 匹配结果（3-5 条，降级时可能更少） */
    private List<HouseMatch> matches;
    /** true = LLM 不可用，结果为本地简单过滤降级 */
    private boolean degraded;

    public static MatchSearchResponse of(List<HouseMatch> matches, boolean degraded) {
        MatchSearchResponse r = new MatchSearchResponse();
        r.setMatches(matches);
        r.setDegraded(degraded);
        return r;
    }
}
