package com.aliren.core;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类（位于 core 模块）。
 * scanBasePackages 必须覆盖整个 com.aliren 树，才能扫描到 rent 等业务模块的
 * Controller / Service 组件；
 * Mapper 扫描同样跨模块：按 @Mapper 注解过滤，避免误注册普通接口（如 DingTalkClient）。
 */
@SpringBootApplication(scanBasePackages = "com.aliren")
@MapperScan(basePackages = "com.aliren", annotationClass = Mapper.class)
public class AlirenApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlirenApplication.class, args);
    }
}
