package com.aliren.auth;

import com.aliren.common.BusinessException;

/** 当前登录用户上下文（ThreadLocal），由 AuthInterceptor 写入/清理 */
public class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前登录用户 id，未登录抛 401 */
    public static Long requireUserId() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BusinessException(401, "未登录");
        }
        return u.getUserId();
    }

    /** 获取当前用户角色，未登录抛 401 */
    public static int requireRole() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BusinessException(401, "未登录");
        }
        return u.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
