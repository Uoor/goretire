package com.aliren.core.interceptor;

import com.aliren.core.auth.UserContext;
import com.aliren.core.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存限流（安全清单项：防接口被刷/营销轰炸）。
 * 按 userid + 接口 做滑动窗口计数，超限抛 429 业务异常（由全局异常处理返回）。
 * 规则覆盖写操作、匹配与所有会触发大模型调用的入口（LLM 成本防护）：
 *   - 发布房源 / 发布求租需求  10 次/小时
 *   - 举报                  5 次/小时
 *   - 一句话找房 / 重新匹配   20 次/小时
 *   - 联系房东 / 联系租客     10 次/小时
 *   - 创建订阅（LLM 解析）    10 次/小时
 *   - 上传                  30 次/小时
 * 注意：单机内存实现，部署多实例时需换 Redis（后续）。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private record Rule(String method, String pattern, int max, long windowMs) {}

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/houses", 10, 3600_000L),
            new Rule("POST", "/api/houses/*/reports", 5, 3600_000L),
            new Rule("POST", "/api/match/search", 20, 3600_000L),
            new Rule("POST", "/api/houses/*/contact", 10, 3600_000L),
            new Rule("POST", "/api/upload", 30, 3600_000L),
            // LLM 成本防护：发布需求 / 重新匹配 / 创建订阅 都会触发大模型调用，限制频率防刷
            new Rule("POST", "/api/demands", 10, 3600_000L),
            new Rule("POST", "/api/demands/*/rematch", 20, 3600_000L),
            new Rule("POST", "/api/subscriptions", 10, 3600_000L),
            new Rule("POST", "/api/demands/*/contact", 10, 3600_000L)
    );

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        if (userId == null) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        for (Rule rule : RULES) {
            if (!rule.method().equalsIgnoreCase(method) || !matcher.match(rule.pattern(), path)) {
                continue;
            }
            String key = userId + ":" + rule.pattern();
            long now = System.currentTimeMillis();
            Deque<Long> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (q) {
                while (!q.isEmpty() && now - q.peekFirst() > rule.windowMs()) {
                    q.pollFirst();
                }
                if (q.size() >= rule.max()) {
                    throw new BusinessException(429, "操作太频繁，请稍后再试");
                }
                q.addLast(now);
            }
            break;
        }
        return true;
    }
}
