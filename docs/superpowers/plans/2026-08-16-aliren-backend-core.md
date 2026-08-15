# 校友安居 · 后端核心骨架实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建「校友安居」后端核心骨架：Spring Boot 项目、MySQL 建表、钉钉免登换 JWT、房源发布/列表/详情/审核/已租出下架完整闭环。

**Architecture:** 标准 Spring Boot 单体分层（Controller → Service → Mapper → MySQL）。JWT 无状态认证，鉴权用 HandlerInterceptor 统一拦截；LLM 匹配、订阅、机器人模块在本计划之后单独实施。

**Tech Stack:** Java 17 · Spring Boot 3.x · MySQL 8 · MyBatis-Plus · jjwt · 钉钉官方 Java SDK · JUnit 5 · Maven

**关联文档：** `zf/backend/2026-08-16-校友安居-后端实现方案.md`（接口清单/表设计）；产品设计见 `zf/2026-08-14-校友安居-产品设计文档.md`

---

## 文件结构总览

```
aliren/
├── pom.xml                          # Maven 依赖
├── src/main/resources/
│   ├── application.yml              # 配置（MySQL/Redis/JWT/钉钉）
│   └── db/schema.sql                # 建表脚本
├── src/main/java/com/aliren/
│   ├── AlirenApplication.java       # 启动类
│   ├── common/
│   │   ├── ApiResponse.java         # 统一响应 {code,msg,data}
│   │   ├── BusinessException.java   # 业务异常
│   │   └── GlobalExceptionHandler.java
│   ├── auth/
│   │   ├── AuthController.java      # POST /api/auth
│   │   ├── AuthService.java         # 免登换号 + 签发 JWT
│   │   ├── JwtUtil.java             # 生成/解析 token
│   │   ├── LoginUser.java           # 当前用户上下文
│   │   └── UserContext.java
│   ├── user/
│   │   ├── User.java                # 实体
│   │   └── UserMapper.java
│   ├── house/
│   │   ├── House.java               # 实体
│   │   ├── HouseMapper.java
│   │   ├── HouseController.java     # 发布/列表/详情/下架
│   │   ├── HouseService.java
│   │   └── dto/
│   │       ├── HouseCreateRequest.java
│   │       ├── HouseResponse.java
│   │       └── HouseListQuery.java
│   ├── admin/
│   │   ├── AdminAuditController.java
│   │   └── AdminAuditService.java   # 审核通过/驳回 + 状态流转
│   └── interceptor/
│       └── AuthInterceptor.java     # JWT 鉴权 + 角色校验
└── src/test/java/com/aliren/         # 各模块测试
```

---

### Task 1: 项目初始化（pom + 启动类 + 配置）

**Files:**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/aliren/AlirenApplication.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>
    <groupId>com.aliren</groupId>
    <artifactId>aliren-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>aliren-backend</name>
    <description>校友安居后端</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <!-- 钉钉官方 SDK -->
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>dingtalk</artifactId>
            <version>2.1.30</version>
        </dependency>
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>tea-openapi</artifactId>
            <version>0.2.4</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- 测试用 H2 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aliren?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

aliren:
  jwt:
    secret: ${JWT_SECRET:aliren-dev-secret-key-please-change-in-prod-0123456789}
    expire-hours: 168
  dingtalk:
    app-key: ${DING_APP_KEY:}
    app-secret: ${DING_APP_SECRET:}
    corp-id: ${DING_CORP_ID:}
```

- [ ] **Step 3: 创建启动类**

```java
package com.aliren;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aliren.**.mapper")
public class AlirenApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlirenApplication.class, args);
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.yml src/main/java/com/aliren/AlirenApplication.java
git commit -m "chore: init spring boot project skeleton"
```

---

### Task 2: 数据库建表

**Files:**
- Create: `src/main/resources/db/schema.sql`

- [ ] **Step 1: 写建表脚本**

```sql
CREATE DATABASE IF NOT EXISTS aliren DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aliren;

CREATE TABLE IF NOT EXISTS `user` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `dingtalk_userid`  VARCHAR(64)  NOT NULL UNIQUE COMMENT '钉钉 userid',
    `nickname`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
    `role`             TINYINT      NOT NULL DEFAULT 0 COMMENT '0=校友 1=管理员',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `house` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `publisher_id`     BIGINT       NOT NULL COMMENT '发布人 user.id',
    `community`        VARCHAR(128) NOT NULL COMMENT '小区',
    `room_no`          VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '房号（仅审核可见）',
    `region`           VARCHAR(32)  NOT NULL COMMENT '区域',
    `house_type`       VARCHAR(32)  NOT NULL COMMENT '户型',
    `area`             INT          NOT NULL COMMENT '面积㎡',
    `rent`             DECIMAL(10,2) NOT NULL COMMENT '月租',
    `deposit_pay`      VARCHAR(16)  NOT NULL COMMENT '押付方式',
    `label`            TINYINT      NOT NULL COMMENT '1=房东直租 2=校友转租 3=合租拼室友',
    `pet_ok`           TINYINT      NOT NULL DEFAULT 0 COMMENT '可养宠',
    `commute`          VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '通勤描述',
    `images`           JSON         NULL COMMENT '图片URL数组',
    `description`      TEXT         NULL COMMENT '描述',
    `audit_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待审核 1=已上架 2=已驳回',
    `audit_reason`     VARCHAR(255) NULL COMMENT '驳回原因',
    `auditor_id`       BIGINT       NULL COMMENT '审核人',
    `audit_time`       DATETIME     NULL,
    `rack_status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=在租中 1=已租出 2=已下架',
    `feedback_answer`  TINYINT      NOT NULL DEFAULT 0 COMMENT '轻问句 0=跳过 1=找到新家 2=暂无',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_list` (`region`, `audit_status`, `rack_status`),
    KEY `idx_publisher` (`publisher_id`)
) ENGINE=InnoDB COMMENT='房源表';

CREATE TABLE IF NOT EXISTS `demand` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `publisher_id`     BIGINT      NOT NULL,
    `budget`           JSON        NULL COMMENT '预算区间',
    `region`           VARCHAR(32) NOT NULL,
    `house_type`       VARCHAR(32) NOT NULL DEFAULT '',
    `move_in_date`     DATE        NULL,
    `lease_term`       VARCHAR(16) NOT NULL DEFAULT '',
    `requirements`     JSON        NULL COMMENT '特殊要求',
    `description`      VARCHAR(500) NOT NULL DEFAULT '',
    `match_status`     TINYINT     NOT NULL DEFAULT 0 COMMENT '0=待匹配 1=已匹配 2=已成交',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_region` (`region`)
) ENGINE=InnoDB COMMENT='求租需求表';

CREATE TABLE IF NOT EXISTS `subscribe` (
    `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`              BIGINT       NOT NULL,
    `type`                 TINYINT      NOT NULL COMMENT '1=找房源 2=找租客',
    `structured_condition` JSON         NULL COMMENT '结构化条件',
    `raw_text`             VARCHAR(500) NOT NULL COMMENT '自然语言原文',
    `status`               TINYINT      NOT NULL DEFAULT 0 COMMENT '0=活跃 1=暂停 2=删除',
    `quiet_hours`          JSON         NULL COMMENT '免打扰时段',
    `push_count`           INT          NOT NULL DEFAULT 0,
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='订阅表';

CREATE TABLE IF NOT EXISTS `report` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `target_type`   TINYINT      NOT NULL COMMENT '1=房源 2=用户',
    `target_id`     BIGINT       NOT NULL,
    `reporter_id`   BIGINT       NOT NULL,
    `reason`        VARCHAR(500) NOT NULL,
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待处理 1=已处理',
    `result`        VARCHAR(500) NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='举报表';

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `operator_id`   BIGINT       NOT NULL,
    `action`        VARCHAR(32)  NOT NULL COMMENT '如 AUDIT_PASS / OFF_RACK',
    `target_type`   VARCHAR(32)  NOT NULL,
    `target_id`     BIGINT       NOT NULL,
    `detail`        JSON         NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='审计日志表';
```

- [ ] **Step 2: 在本地 MySQL 执行建表**

Run: `mysql -uroot -p < src/main/resources/db/schema.sql`
Expected: 6 张表创建成功

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/schema.sql
git commit -m "feat: add mysql schema for core tables"
```

---

### Task 3: 统一响应 + 全局异常

**Files:**
- Create: `src/main/java/com/aliren/common/ApiResponse.java`
- Create: `src/main/java/com/aliren/common/BusinessException.java`
- Create: `src/main/java/com/aliren/common/GlobalExceptionHandler.java`

- [ ] **Step 1: 写测试**

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=ApiResponseTest`
Expected: 编译失败（ApiResponse 不存在）

- [ ] **Step 3: 实现三个类**

```java
package com.aliren.common;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
```

```java
package com.aliren.common;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
```

```java
package com.aliren.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("参数错误");
        return ApiResponse.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleOther(Exception e) {
        log.error("unexpected error", e);
        return ApiResponse.error(500, "服务异常");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q test -Dtest=ApiResponseTest`
Expected: PASS（2 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aliren/common/
git commit -m "feat: unified api response and global exception handler"
```

---

### Task 4: JWT 工具类

**Files:**
- Create: `src/main/java/com/aliren/auth/JwtUtil.java`

- [ ] **Step 1: 写测试**

```java
package com.aliren.auth;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generateAndParse_roundTrip() {
        JwtUtil jwt = new JwtUtil("aliren-test-secret-key-0123456789abcdef0123456789", 168);
        String token = jwt.generate(42L, "ding-123", 0);
        LoginUser user = jwt.parse(token);
        assertThat(user.getUserId()).isEqualTo(42L);
        assertThat(user.getDingtalkUserId()).isEqualTo("ding-123");
        assertThat(user.getRole()).isZero();
    }

    @Test
    void parse_invalidToken_returnsNull() {
        JwtUtil jwt = new JwtUtil("aliren-test-secret-key-0123456789abcdef0123456789", 168);
        assertThat(jwt.parse("invalid.token.here")).isNull();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=JwtUtilTest`
Expected: 编译失败（JwtUtil 不存在）

- [ ] **Step 3: 实现 JwtUtil 和 LoginUser**

```java
package com.aliren.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${aliren.jwt.secret}") String secret,
                   @Value("${aliren.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generate(Long userId, String dingtalkUserId, int role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("dingUserId", dingtalkUserId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            LoginUser u = new LoginUser();
            u.setUserId(Long.valueOf(claims.getSubject()));
            u.setDingtalkUserId(claims.get("dingUserId", String.class));
            u.setRole(claims.get("role", Integer.class));
            return u;
        } catch (Exception e) {
            return null;
        }
    }
}
```

```java
package com.aliren.auth;

import lombok.Data;

@Data
public class LoginUser {
    private Long userId;
    private String dingtalkUserId;
    private int role;
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q test -Dtest=JwtUtilTest`
Expected: PASS（2 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aliren/auth/JwtUtil.java src/main/java/com/aliren/auth/LoginUser.java src/test/java/com/aliren/auth/JwtUtilTest.java
git commit -m "feat: jwt generate and parse"
```

---

### Task 5: 用户表 + 免登接口

**Files:**
- Create: `src/main/java/com/aliren/user/User.java`
- Create: `src/main/java/com/aliren/user/UserMapper.java`
- Create: `src/main/java/com/aliren/auth/AuthController.java`
- Create: `src/main/java/com/aliren/auth/AuthService.java`
- Create: `src/main/java/com/aliren/auth/DingTalkClient.java`
- Create: `src/main/java/com/aliren/auth/dto/AuthResponse.java`
- Test: `src/test/java/com/aliren/auth/AuthServiceTest.java`

- [ ] **Step 1: 写测试（Mockito 模拟钉钉换号）**

```java
package com.aliren.auth;

import com.aliren.common.BusinessException;
import com.aliren.user.User;
import com.aliren.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private DingTalkClient dingTalkClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, dingTalkClient, new JwtUtil(
                "aliren-test-secret-key-0123456789abcdef0123456789", 168));
    }

    @Test
    void auth_newUser_createsAndReturnsToken() {
        when(dingTalkClient.getUserIdByCode("code-1")).thenReturn("ding-100");
        when(userMapper.selectByDingtalkUserId("ding-100")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(1L);
            return 1;
        });

        AuthResponse resp = authService.authenticate("code-1");
        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getUser().getUserId()).isEqualTo(1L);
        assertThat(resp.getUser().getRole()).isZero();
    }

    @Test
    void auth_invalidCode_throws() {
        when(dingTalkClient.getUserIdByCode("bad")).thenThrow(new BusinessException(401, "免登失败"));
        assertThatThrownBy(() -> authService.authenticate("bad"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("免登失败");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=AuthServiceTest`
Expected: 编译失败（AuthService/DingTalkClient 不存在）

- [ ] **Step 3: 实现**

```java
package com.aliren.auth;

public interface DingTalkClient {
    String getUserIdByCode(String authCode);
}
```

```java
package com.aliren.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dingtalkUserId;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
package com.aliren.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

public interface UserMapper extends BaseMapper<User> {
    default User selectByDingtalkUserId(String dingtalkUserId) {
        return selectOne(new QueryWrapper<User>().eq("dingtalk_userid", dingtalkUserId));
    }
}
```

```java
package com.aliren.auth;

import com.aliren.auth.dto.AuthResponse;
import com.aliren.common.BusinessException;
import com.aliren.user.User;
import com.aliren.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final DingTalkClient dingTalkClient;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, DingTalkClient dingTalkClient, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.dingTalkClient = dingTalkClient;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse authenticate(String authCode) {
        String dingtalkUserId;
        try {
            dingtalkUserId = dingTalkClient.getUserIdByCode(authCode);
        } catch (Exception e) {
            throw new BusinessException(401, "免登失败");
        }
        User user = userMapper.selectByDingtalkUserId(dingtalkUserId);
        if (user == null) {
            user = new User();
            user.setDingtalkUserId(dingtalkUserId);
            user.setNickname("校友");
            user.setRole(0);
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已停用");
        }
        String token = jwtUtil.generate(user.getId(), user.getDingtalkUserId(), user.getRole());
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setUserId(user.getId());
        info.setNickname(user.getNickname());
        info.setRole(user.getRole());
        return new AuthResponse(token, info);
    }
}
```

```java
package com.aliren.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private int role;
    }
}
```

```java
package com.aliren.auth;

import com.aliren.auth.dto.AuthResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public AuthResponse auth(@RequestBody AuthRequest req) {
        return authService.authenticate(req.getCode());
    }

    @Data
    public static class AuthRequest {
        @NotBlank(message = "code 不能为空")
        private String code;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q test -Dtest=AuthServiceTest`
Expected: PASS（2 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aliren/user/ src/main/java/com/aliren/auth/
git commit -m "feat: dingtalk auth code exchange with jwt"
```

---

### Task 6: 钉钉换号真实实现（DingTalkClientImpl，骨架）

**Files:**
- Create: `src/main/java/com/aliren/auth/DingTalkClientImpl.java`

- [ ] **Step 1: 实现（access_token 获取 + 换号占位）**

```java
package com.aliren.auth;

import com.aliren.common.BusinessException;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.taobao.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 钉钉免登换号：
 * 1) appKey/appSecret 换 access_token（已实现）
 * 2) authCode → unionId（H5 免登接口，待按当前 SDK 补全）
 * 3) unionId → userid（企业内部应用，待补全）
 * 参考: https://open.dingtalk.com/document/orgapp/logon-free-process
 */
@Component
public class DingTalkClientImpl implements DingTalkClient {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";

    private final String appKey;
    private final String appSecret;

    public DingTalkClientImpl(@Value("${aliren.dingtalk.app-key}") String appKey,
                              @Value("${aliren.dingtalk.app-secret}") String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    @Override
    public String getUserIdByCode(String authCode) {
        String accessToken = getAccessToken();
        // TODO(Task 6 完成点): 按钉钉当前 SDK 实现 authCode -> unionId -> userid
        throw new BusinessException(500, "钉钉换号待接入：需配置 DING_APP_KEY / DING_APP_SECRET 并按开放平台文档实现");
    }

    private String getAccessToken() {
        DingTalkClient client = new DefaultDingTalkClient(GET_TOKEN_URL);
        OapiGettokenRequest req = new OapiGettokenRequest();
        req.setAppkey(appKey);
        req.setAppsecret(appSecret);
        try {
            OapiGettokenResponse resp = client.execute(req);
            if (resp.getErrcode() != 0) {
                throw new BusinessException(500, "获取钉钉token失败: " + resp.getErrmsg());
            }
            return resp.getAccessToken();
        } catch (ApiException e) {
            throw new BusinessException(500, "获取钉钉token异常: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aliren/auth/DingTalkClientImpl.java
git commit -m "feat: dingtalk client skeleton with access token"
```

> 说明：authCode→unionId→userid 两步需真实钉钉应用凭证，本任务先搭骨架；拿到凭证后按开放平台文档补全（Task 6 完成点已标注）。

---

### Task 7: 鉴权拦截器

**Files:**
- Create: `src/main/java/com/aliren/interceptor/AuthInterceptor.java`
- Create: `src/main/java/com/aliren/config/WebConfig.java`
- Test: `src/test/java/com/aliren/interceptor/AuthInterceptorTest.java`

- [ ] **Step 1: 写测试**

```java
package com.aliren.interceptor;

import com.aliren.auth.JwtUtil;
import com.aliren.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthInterceptorTest {

    private AuthInterceptor interceptor;
    private JwtUtil jwt;

    @BeforeEach
    void setUp() {
        jwt = new JwtUtil("aliren-test-secret-key-0123456789abcdef0123456789", 168);
        interceptor = new AuthInterceptor(jwt);
        UserContext.clear();
    }

    @Test
    void validToken_passAndSetContext() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt.generate(1L, "ding-1", 0));
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean ok = interceptor.preHandle(req, resp, new Object());
        assertThat(ok).isTrue();
        assertThat(UserContext.get().getUserId()).isEqualTo(1L);
    }

    @Test
    void missingToken_reject() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean ok = interceptor.preHandle(req, resp, new Object());
        assertThat(ok).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=AuthInterceptorTest`
Expected: 编译失败

- [ ] **Step 3: 实现**

```java
package com.aliren.auth;

public class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long requireUserId() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new com.aliren.common.BusinessException(401, "未登录");
        }
        return u.getUserId();
    }

    public static int requireRole() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new com.aliren.common.BusinessException(401, "未登录");
        }
        return u.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
```

```java
package com.aliren.interceptor;

import com.aliren.auth.JwtUtil;
import com.aliren.auth.LoginUser;
import com.aliren.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            LoginUser user = jwtUtil.parse(auth.substring(7));
            if (user != null) {
                UserContext.set(user);
                return true;
            }
        }
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
```

```java
package com.aliren.config;

import com.aliren.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth", "/error");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q test -Dtest=AuthInterceptorTest`
Expected: PASS（2 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aliren/auth/UserContext.java src/main/java/com/aliren/interceptor/ src/main/java/com/aliren/config/ src/test/java/com/aliren/interceptor/
git commit -m "feat: jwt auth interceptor"
```

---

### Task 8: 房源发布 + 列表 + 详情

**Files:**
- Create: `src/main/java/com/aliren/house/House.java`
- Create: `src/main/java/com/aliren/house/HouseMapper.java`
- Create: `src/main/java/com/aliren/house/dto/HouseCreateRequest.java`
- Create: `src/main/java/com/aliren/house/dto/HouseResponse.java`
- Create: `src/main/java/com/aliren/house/dto/HouseListQuery.java`
- Create: `src/main/java/com/aliren/house/HouseService.java`
- Create: `src/main/java/com/aliren/house/HouseController.java`
- Test: `src/test/java/com/aliren/house/HouseServiceTest.java`

- [ ] **Step 1: 写测试**

```java
package com.aliren.house;

import com.aliren.common.BusinessException;
import com.aliren.house.dto.HouseCreateRequest;
import com.aliren.house.dto.HouseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock
    private HouseMapper houseMapper;

    private HouseService houseService;

    @BeforeEach
    void setUp() {
        houseService = new HouseService(houseMapper);
    }

    @Test
    void publish_createsHouseWithPendingAudit() {
        HouseCreateRequest req = new HouseCreateRequest();
        req.setCommunity("西溪八方城");
        req.setRegion("杭州西溪");
        req.setHouseType("2室1厅");
        req.setArea(89);
        req.setRent(5800);
        req.setDepositPay("押一付三");
        req.setLabel(1);
        when(houseMapper.insert(any(House.class))).thenAnswer(inv -> {
            inv.getArgument(0, House.class).setId(10L);
            return 1;
        });

        Long id = houseService.publish(7L, req);
        assertThat(id).isEqualTo(10L);

        ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).insert(captor.capture());
        House saved = captor.getValue();
        assertThat(saved.getPublisherId()).isEqualTo(7L);
        assertThat(saved.getAuditStatus()).isZero(); // 待审核
        assertThat(saved.getRackStatus()).isZero();  // 在租中
    }

    @Test
    void publish_negativeRent_rejected() {
        HouseCreateRequest req = new HouseCreateRequest();
        req.setRent(java.math.BigDecimal.valueOf(-1));
        assertThatThrownBy(() -> houseService.publish(7L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void detail_notAudited_forbidden() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(0);
        when(houseMapper.selectById(1L)).thenReturn(h);
        assertThatThrownBy(() -> houseService.detail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void list_auditedOnly() {
        when(houseMapper.selectList(any())).thenReturn(List.of());
        List<HouseResponse> list = houseService.list(new com.aliren.house.dto.HouseListQuery());
        assertThat(list).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=HouseServiceTest`
Expected: 编译失败

- [ ] **Step 3: 实现**

```java
package com.aliren.house;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("house")
public class House {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private String community;
    private String roomNo;
    private String region;
    private String houseType;
    private Integer area;
    private BigDecimal rent;
    private String depositPay;
    private Integer label;
    private Integer petOk;
    private String commute;
    private String images;
    private String description;
    private Integer auditStatus;
    private String auditReason;
    private Long auditorId;
    private LocalDateTime auditTime;
    private Integer rackStatus;
    private Integer feedbackAnswer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
package com.aliren.house;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface HouseMapper extends BaseMapper<House> {
}
```

```java
package com.aliren.house.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseCreateRequest {
    @NotBlank(message = "小区不能为空")
    private String community;
    private String roomNo;
    @NotBlank(message = "区域不能为空")
    private String region;
    @NotBlank(message = "户型不能为空")
    private String houseType;
    @NotNull(message = "面积不能为空")
    private Integer area;
    @NotNull(message = "租金不能为空")
    @DecimalMin(value = "0.01", message = "租金不合法")
    private BigDecimal rent;
    @NotBlank(message = "押付方式不能为空")
    private String depositPay;
    @NotNull(message = "标签不能为空")
    private Integer label;
    private Integer petOk;
    private String commute;
    private String images;
    private String description;
}
```

```java
package com.aliren.house.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HouseResponse {
    private Long id;
    private String community;
    private String region;
    private String houseType;
    private Integer area;
    private BigDecimal rent;
    private String depositPay;
    private Integer label;
    private Integer petOk;
    private String commute;
    private List<String> images;
    private String description;
    private Integer auditStatus;
    private Integer rackStatus;
    private LocalDateTime createdAt;

    public static HouseResponse from(House h) {
        HouseResponse r = new HouseResponse();
        r.setId(h.getId());
        r.setCommunity(h.getCommunity());
        r.setRegion(h.getRegion());
        r.setHouseType(h.getHouseType());
        r.setArea(h.getArea());
        r.setRent(h.getRent());
        r.setDepositPay(h.getDepositPay());
        r.setLabel(h.getLabel());
        r.setPetOk(h.getPetOk());
        r.setCommute(h.getCommute());
        r.setDescription(h.getDescription());
        r.setAuditStatus(h.getAuditStatus());
        r.setRackStatus(h.getRackStatus());
        r.setCreatedAt(h.getCreatedAt());
        return r;
    }
}
```

```java
package com.aliren.house.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseListQuery {
    private String region;
    private BigDecimal minRent;
    private BigDecimal maxRent;
    private Integer label;
    private Integer petOk;
}
```

```java
package com.aliren.house;

import com.aliren.common.BusinessException;
import com.aliren.house.dto.HouseCreateRequest;
import com.aliren.house.dto.HouseListQuery;
import com.aliren.house.dto.HouseResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class HouseService {

    private final HouseMapper houseMapper;

    public HouseService(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
    }

    public Long publish(Long publisherId, HouseCreateRequest req) {
        if (req.getRent() == null || req.getRent().signum() <= 0) {
            throw new BusinessException("租金不合法");
        }
        House h = new House();
        h.setPublisherId(publisherId);
        h.setCommunity(req.getCommunity());
        h.setRoomNo(req.getRoomNo());
        h.setRegion(req.getRegion());
        h.setHouseType(req.getHouseType());
        h.setArea(req.getArea());
        h.setRent(req.getRent());
        h.setDepositPay(req.getDepositPay());
        h.setLabel(req.getLabel());
        h.setPetOk(req.getPetOk() == null ? 0 : req.getPetOk());
        h.setCommute(req.getCommute());
        h.setImages(req.getImages());
        h.setDescription(req.getDescription());
        h.setAuditStatus(0);
        h.setRackStatus(0);
        h.setFeedbackAnswer(0);
        houseMapper.insert(h);
        return h.getId();
    }

    public List<HouseResponse> list(HouseListQuery query) {
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("audit_status", 1).eq("rack_status", 0);
        if (StringUtils.hasText(query.getRegion())) {
            qw.eq("region", query.getRegion());
        }
        if (query.getMaxRent() != null) {
            qw.le("rent", query.getMaxRent());
        }
        if (query.getMinRent() != null) {
            qw.ge("rent", query.getMinRent());
        }
        if (query.getLabel() != null) {
            qw.eq("label", query.getLabel());
        }
        if (query.getPetOk() != null) {
            qw.eq("pet_ok", query.getPetOk());
        }
        qw.orderByDesc("created_at");
        return houseMapper.selectList(qw).stream().map(HouseResponse::from).toList();
    }

    public HouseResponse detail(Long id) {
        House h = houseMapper.selectById(id);
        if (h == null || h.getAuditStatus() != 1) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        return HouseResponse.from(h);
    }
}
```

```java
package com.aliren.house;

import com.aliren.common.ApiResponse;
import com.aliren.auth.UserContext;
import com.aliren.house.dto.HouseCreateRequest;
import com.aliren.house.dto.HouseListQuery;
import com.aliren.house.dto.HouseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
public class HouseController {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @GetMapping
    public ApiResponse<List<HouseResponse>> list(HouseListQuery query) {
        return ApiResponse.ok(houseService.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<HouseResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(houseService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> publish(@Valid @RequestBody HouseCreateRequest req) {
        Long id = houseService.publish(UserContext.requireUserId(), req);
        return ApiResponse.ok(id);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q test -Dtest=HouseServiceTest`
Expected: PASS（4 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aliren/house/ src/test/java/com/aliren/house/
git commit -m "feat: house publish list detail"
```

---

### Task 9: 管理后台审核（通过/驳回）+ 已租出下架

**Files:**
- Create: `src/main/java/com/aliren/admin/AdminAuditController.java`
- Create: `src/main/java/com/aliren/admin/AdminAuditService.java`
- Create: `src/main/java/com/aliren/admin/dto/AuditRequest.java`
- Test: `src/test/java/com/aliren/admin/AdminAuditServiceTest.java`

- [ ] **Step 1: 写测试**

```java
package com.aliren.admin;

import com.aliren.common.BusinessException;
import com.aliren.house.House;
import com.aliren.house.HouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock
    private HouseMapper houseMapper;

    private AdminAuditService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditService(houseMapper);
    }

    @Test
    void audit_nonAdmin_rejected() {
        assertThatThrownBy(() -> service.audit(0, 1L, true, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限");
    }

    @Test
    void audit_pass_setsAudited() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(0);
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.audit(1, 1L, true, null);
        verify(houseMapper).updateById(any(House.class));
    }

    @Test
    void audit_reject_requiresReason() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(0);
        when(houseMapper.selectById(1L)).thenReturn(h);

        assertThatThrownBy(() -> service.audit(1, 1L, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("驳回原因");
    }

    @Test
    void offRack_nonOwnerNonAdmin_rejected() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        when(houseMapper.selectById(1L)).thenReturn(h);

        assertThatThrownBy(() -> service.offRack(8L, 0, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限");
    }

    @Test
    void offRack_owner_succeeds() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.offRack(7L, 0, 1L);
        verify(houseMapper).updateById(any(House.class));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=AdminAuditServiceTest`
Expected: 编译失败

- [ ] **Step 3: 实现**

```java
package com.aliren.admin;

import com.aliren.common.BusinessException;
import com.aliren.house.House;
import com.aliren.house.HouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AdminAuditService {

    private final HouseMapper houseMapper;

    public AdminAuditService(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
    }

    @Transactional
    public void audit(int operatorRole, Long houseId, boolean pass, String reason) {
        if (operatorRole != 1) {
            throw new BusinessException(403, "无权限：仅管理员可审核");
        }
        House h = requirePending(houseId);
        if (pass) {
            h.setAuditStatus(1);
            h.setAuditReason(null);
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException("驳回原因不能为空");
            }
            h.setAuditStatus(2);
            h.setAuditReason(reason);
        }
        h.setAuditTime(LocalDateTime.now());
        houseMapper.updateById(h);
        // TODO(Task 10+): 审核通过后触发「机器人推卡片 + 订阅匹配」
    }

    @Transactional
    public void offRack(Long operatorId, int operatorRole, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean isOwner = h.getPublisherId().equals(operatorId);
        boolean isAdmin = operatorRole == 1;
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权限：仅发布人或管理员可下架");
        }
        h.setRackStatus(1); // 已租出
        houseMapper.updateById(h);
    }

    private House requirePending(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != 0) {
            throw new BusinessException(404, "房源不存在或不在待审核状态");
        }
        return h;
    }
}
```

```java
package com.aliren.admin;

import com.aliren.admin.dto.AuditRequest;
import com.aliren.common.ApiResponse;
import com.aliren.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @PostMapping("/audit/{houseId}")
    public ApiResponse<Void> audit(@PathVariable Long houseId,
                                   @Valid @RequestBody AuditRequest req) {
        adminAuditService.audit(UserContext.requireRole(), houseId, req.isPass(), req.getReason());
        return ApiResponse.ok(null);
    }

    @PostMapping("/houses/{houseId}/off-rack")
    public ApiResponse<Void> offRack(@PathVariable Long houseId) {
        adminAuditService.offRack(UserContext.requireUserId(), UserContext.requireRole(), houseId);
        return ApiResponse.ok(null);
    }
}
```

```java
package com.aliren.admin.dto;

import lombok.Data;

@Data
public class AuditRequest {
    private boolean pass;
    private String reason;
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q test -Dtest=AdminAuditServiceTest`
Expected: PASS（5 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aliren/admin/ src/test/java/com/aliren/admin/
git commit -m "feat: admin audit and off-rack"
```

---

## 验收标准（本计划完成 = 满足以下全部）

- [ ] `mvn test` 全绿（本项目 15+ 个测试）
- [ ] 本地 MySQL 执行 schema.sql 后 6 张表可用
- [ ] `POST /api/auth` 可换号（真实钉钉凭证接入后）；无凭证时返回明确错误
- [ ] 未带 token 访问 `/api/houses` 返回 401
- [ ] 校友可发布房源（待审核）、看已上架列表与详情
- [ ] 管理员可审核通过/驳回（驳回必填原因）
- [ ] 发布人/管理员可将房源"已租出下架"
- [ ] 全部提交已 commit（含测试）

## 后续计划（不在本计划内，按同一模式扩展）

1. **house 扩展**：详情响应含房东信息、举报接口、audit_log 落库
2. **demand 模块**：求租需求 CRUD + 求租墙
3. **subscribe 模块**：订阅 CRUD + 免打扰 + 批量推送
4. **match 模块**：LLM 一句话找房 / 订阅批量匹配 / 求租墙匹配
5. **robot 模块**：Stream 长连接 + 交互卡片 + 工作通知
6. **schedule**：周五周推 / 周一安居故事
7. **前端**：Vue3 H5（见 frontend 方案）
