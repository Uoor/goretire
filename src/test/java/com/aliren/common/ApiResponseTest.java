package com.aliren.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_returnsCodeZeroWithData() {
        ApiResponse<String> resp = ApiResponse.ok("hello");
        assertThat(resp.getCode()).isZero();
        assertThat(resp.getData()).isEqualTo("hello");
    }

    @Test
    void error_returnsCodeAndMsg() {
        ApiResponse<Void> resp = ApiResponse.error(400, "参数错误");
        assertThat(resp.getCode()).isEqualTo(400);
        assertThat(resp.getMsg()).isEqualTo("参数错误");
    }
}
