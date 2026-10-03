package com.aliren.houserent.match.client;

/**
 * LLM 匹配客户端抽象。
 * 实现：QwenMatchClient（配置 aliren.llm.api-key 后启用）；MatchClientStub（缺省，返回 null 触发降级）。
 * 调用方（MatchService）负责 prompt 组装与输出 JSON 解析，失败时降级为本地简单过滤，不阻断主流程。
 */
public interface MatchClient {

    /**
     * 给定 prompt 返回模型输出文本。
     *
     * @return 模型输出；不可用/调用失败返回 null，由调用方降级处理
     */
    String complete(String prompt);
}
