-- ============================================================
-- H2（MySQL 兼容模式）建表脚本 —— 仅用于本地无 MySQL 时联调
-- 用法：java -jar aliren-app-*.jar \
--   --spring.datasource.url=jdbc:h2:mem:aliren\;MODE=MySQL\;DB_CLOSE_DELAY=-1\;DATABASE_TO_LOWER=TRUE \
--   --spring.datasource.driver-class-name=org.h2.Driver \
--   --spring.sql.init.mode=always \
--   --spring.sql.init.schema-locations=classpath:db/schema-h2.sql \
--   --spring.sql.init.data-locations=classpath:db/data-h2.sql
-- 生产环境使用 src/main/resources/db/schema.sql（MySQL 8）
-- ============================================================

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS subscribe;
DROP TABLE IF EXISTS demand;
DROP TABLE IF EXISTS house;
DROP TABLE IF EXISTS "user";

CREATE TABLE "user" (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  dingtalk_userid   VARCHAR(64)  NOT NULL UNIQUE,
  nickname          VARCHAR(64)  NOT NULL DEFAULT '',
  avatar            VARCHAR(255) NOT NULL DEFAULT '',
  role              TINYINT      NOT NULL DEFAULT 0,
  status            TINYINT      NOT NULL DEFAULT 1,
  created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE house (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  publisher_id     BIGINT       NOT NULL,
  community        VARCHAR(128) NOT NULL,
  room_no          VARCHAR(32)  NOT NULL DEFAULT '',
  region           VARCHAR(32)  NOT NULL,
  house_type       VARCHAR(32)  NOT NULL,
  area             INT          NOT NULL,
  rent             DECIMAL(10,2) NOT NULL,
  deposit_pay      VARCHAR(16)  NOT NULL,
  label            TINYINT      NOT NULL,
  pet_ok           TINYINT      NOT NULL DEFAULT 0,
  commute          VARCHAR(64)  NOT NULL DEFAULT '',
  images           JSON,
  description      CLOB,
  audit_status     TINYINT      NOT NULL DEFAULT 0,
  audit_reason     VARCHAR(255),
  auditor_id       BIGINT,
  audit_time       TIMESTAMP,
  rack_status      TINYINT      NOT NULL DEFAULT 0,
  feedback_answer  TINYINT      NOT NULL DEFAULT 0,
  created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE demand (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  publisher_id  BIGINT      NOT NULL,
  budget        JSON,
  region        VARCHAR(32) NOT NULL,
  house_type    VARCHAR(32) NOT NULL DEFAULT '',
  move_in_date  DATE,
  lease_term    VARCHAR(16) NOT NULL DEFAULT '',
  requirements  JSON,
  description   VARCHAR(500) NOT NULL DEFAULT '',
  match_status  TINYINT     NOT NULL DEFAULT 0,
  created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscribe (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id              BIGINT       NOT NULL,
  type                 TINYINT      NOT NULL,
  structured_condition JSON,
  raw_text             VARCHAR(500) NOT NULL,
  status               TINYINT      NOT NULL DEFAULT 0,
  quiet_hours          JSON,
  push_count           INT          NOT NULL DEFAULT 0,
  created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  target_type TINYINT      NOT NULL,
  target_id   BIGINT       NOT NULL,
  reporter_id BIGINT       NOT NULL,
  reason      VARCHAR(500) NOT NULL,
  status      TINYINT      NOT NULL DEFAULT 0,
  result      VARCHAR(500),
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id BIGINT      NOT NULL,
  action      VARCHAR(32) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id   BIGINT      NOT NULL,
  detail      JSON,
  created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
