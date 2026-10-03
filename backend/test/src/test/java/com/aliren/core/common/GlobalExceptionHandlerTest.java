package com.aliren.core.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        ApiResponse<Void> resp = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/nonexistent"));
        assertThat(resp.getCode()).isEqualTo(404);
    }

    @Test
    void handleTypeMismatch_returns400() {
        ApiResponse<Void> resp = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null));
        assertThat(resp.getCode()).isEqualTo(400);
        assertThat(resp.getMsg()).contains("id");
    }

    @Test
    void handleNotReadable_returns400() {
        ApiResponse<Void> resp = handler.handleNotReadable(
                new HttpMessageNotReadableException("bad json"));
        assertThat(resp.getCode()).isEqualTo(400);
    }

    @Test
    void handleBusiness_returnsItsCode() {
        ApiResponse<Void> resp = handler.handleBusiness(new BusinessException(403, "无权限"));
        assertThat(resp.getCode()).isEqualTo(403);
        assertThat(resp.getMsg()).isEqualTo("无权限");
    }
}
