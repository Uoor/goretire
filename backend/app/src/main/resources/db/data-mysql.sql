-- ============================================================
-- MySQL 种子数据（开发联调）：dev-code 管理员 + 房源 + 需求 + 订阅
-- 用法：mysql -u root -p aliren < data-mysql.sql
-- ============================================================

-- dev-code 用户：前端浏览器免登桩以 authCode='dev-code' 换身份，设为管理员便于联调
INSERT INTO `user` (id, dingtalk_userid, nickname, avatar, role, status) VALUES
  (1, 'dev-code', '校友小明', '', 1, 1),
  (2, 'ding-admin', '运营大管家', '', 1, 1),
  (3, 'ding-owner-1', '房东老王', '', 0, 1),
  (4, 'ding-owner-2', '转租小李', '', 0, 1);

INSERT INTO house (id, publisher_id, community, room_no, region, house_type, area, rent, deposit_pay, label, pet_ok, commute, images, description, audit_status, rack_status, feedback_answer) VALUES
  (1, 3, '西溪八方城', '8-1201', '杭州西溪', '2室1厅', 89, 5800, '押一付三', 1, 1, '西溪园区 15 分钟', '["https://picsum.photos/seed/h1/400/300"]', '房东自住刚搬走，家具全，可拎包入住。南向采光好，近西溪园区。', 1, 0, 0),
  (2, 4, '滨江长河', '3-502', '杭州滨江', '1室1厅', 65, 4200, '押一付一', 2, 0, '滨江园区 20 分钟', '["https://picsum.photos/seed/h2/400/300"]', '原房转租，随时入住，限校友。', 1, 0, 0),
  (3, 3, '西溪蝶园', '7-1603', '杭州西溪', '主卧合租', 22, 2600, '押一付一', 3, 0, '西溪园区 10 分钟', NULL, '合租拼室友，限 1 人，室友均为校友，作息规律。', 1, 0, 0),
  (4, 4, '北京望京', '2-801', '北京望京', '2室1厅', 78, 7500, '押一付三', 1, 1, '望京园区 25 分钟', '["https://picsum.photos/seed/h4/400/300"]', '整租两居，新装修，通勤便利。', 0, 0, 0),
  (5, 3, '上海张江', '5-1102', '上海张江', '1室1厅', 55, 5200, '押二付一', 1, 0, '张江园区 12 分钟', NULL, '已租出，作为轻问句样例。', 1, 1, 1);

INSERT INTO demand (id, publisher_id, budget, region, house_type, move_in_date, lease_term, requirements, description, match_status) VALUES
  (1, 4, '{"min":4000,"max":6000}', '杭州西溪', '2室1厅', '2026-09-01', '一年', '["可养宠","带车位"]', '西溪附近两居，预算 6000 内，能养猫最好。', 0),
  (2, 1, '{"min":2000,"max":3000}', '杭州滨江', '主卧', NULL, '半年', NULL, '滨江合租主卧，价格 2500 左右。', 0);

INSERT INTO subscribe (id, user_id, type, structured_condition, raw_text, status, push_count) VALUES
  (1, 1, 1, '{"region":"杭州西溪","maxRent":6000}', '西溪附近 6000 以内两居，能养猫', 0, 0),
  (2, 3, 2, '{"region":"杭州"}', '想找西溪两居的租客', 0, 0),
  (3, 4, 1, NULL, '北京望京两居 7000 以内', 1, 1);
