package com.aliren.core.config;

import com.aliren.core.interceptor.AuthInterceptor;
import com.aliren.core.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final String uploadDir;

    public WebConfig(AuthInterceptor authInterceptor, RateLimitInterceptor rateLimitInterceptor,
                     @Value("${aliren.upload.dir:./uploads}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // 免登、JSAPI 签名/诊断、避坑指南（公开内容）免鉴权
                .excludePathPatterns("/api/auth", "/api/dingtalk/jsapi-sign", "/api/dingtalk/jsapi-debug", "/api/guide/**", "/error");
        // 限流在鉴权之后（依赖 UserContext）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth", "/api/dingtalk/jsapi-sign", "/api/dingtalk/jsapi-debug", "/api/guide/**", "/error");
    }

    /** 上传目录静态映射：/uploads/** → 本地目录 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
