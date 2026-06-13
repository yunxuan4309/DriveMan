-- ============================================
-- 数据库升级脚本: 基础数据管理优化
-- 说明: 车型扩展 + 考场独立 + 特种车辆考试
-- 适用数据库: driveman
-- 执行方式: mysql -u root -proot driveman < upgrade_basic_data.sql
-- ============================================

USE `driveman`;

-- ============================================
-- 1. license_config — 车型科目配置表扩展
--    exam_mode: 1-小汽车(默认), 2-特种车辆(理论+实操)
--    coach_audit_required: 报名考试是否需要教练审核
--    cert_name: 获证名称(仅特种车辆使用)
-- ============================================
ALTER TABLE `license_config`
  ADD COLUMN `exam_mode` TINYINT NOT NULL DEFAULT 1
    COMMENT '考试模式: 1-小汽车(科一~科四), 2-特种车辆(理论+实操)'
  AFTER `sort_order`,
  ADD COLUMN `coach_audit_required` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '报名考试是否需要教练审核: 1-需要, 0-不需要'
  AFTER `exam_mode`,
  ADD COLUMN `cert_name` VARCHAR(50) DEFAULT NULL
    COMMENT '获证名称(仅特种车辆使用, 如"叉车操作证")'
  AFTER `coach_audit_required`;

-- ============================================
-- 2. 插入特种车辆车型配置数据
--    叉车 N1 / 挖掘机 N2 / 压路机 N3
--    理论 (subject=1) + 实操 (subject=2)
-- ============================================
INSERT IGNORE INTO `license_config`
    (`license_type`, `subject`, `required_hours`, `exam_items`, `description`,
     `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`)
VALUES
-- 叉车 N1
('N1', 1, 0,  '安全法规,叉车基础知识',              '叉车理论考试', 1, 2, 0, '叉车操作证'),
('N1', 2, 20, '起步,货叉装卸,倒车入库,停放',         '叉车实操考试', 2, 2, 0, NULL),
-- 挖掘机 N2
('N2', 1, 0,  '安全法规,挖掘机基础知识',              '挖掘机理论考试', 1, 2, 0, '挖掘机操作证'),
('N2', 2, 25, '挖沟,平整,装车,上下板车,行走',        '挖掘机实操考试', 2, 2, 0, NULL),
-- 压路机 N3
('N3', 1, 0,  '安全法规,压路机基础知识',              '压路机理论考试', 1, 2, 0, '压路机操作证'),
('N3', 2, 20, '起步,压实作业,转向,掉头,停放',        '压路机实操考试', 2, 2, 0, NULL);

-- ============================================
-- 3. exam_venue — 新建考场信息表
-- ============================================
CREATE TABLE IF NOT EXISTS `exam_venue` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '考场ID',
    `name` VARCHAR(100) NOT NULL COMMENT '考场名称',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `capacity` INT UNSIGNED DEFAULT NULL COMMENT '容纳人数',
    `facilities` VARCHAR(500) DEFAULT NULL COMMENT '设施设备说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考场信息表';

-- ============================================
-- 4. exam_session — 关联考场ID
-- ============================================
ALTER TABLE `exam_session`
  ADD COLUMN `venue_id` INT UNSIGNED DEFAULT NULL COMMENT '关联考场ID'
  AFTER `location`,
  ADD KEY `idx_venue_id` (`venue_id`);

-- 迁移现有数据: 将现有 location 转为考场记录
INSERT IGNORE INTO `exam_venue` (`name`, `address`, `capacity`, `status`) VALUES
('南岸区车管所', '南岸区', 100, 1),
('南坪科目二考场', '南坪', 80, 1),
('八公里科目三考场', '八公里', 60, 1);

-- 将考场ID回填到 exam_session
UPDATE `exam_session` e
JOIN `exam_venue` v ON e.`location` = v.`name`
SET e.`venue_id` = v.`id`
WHERE e.`venue_id` IS NULL;

-- ============================================
-- 5. special_exam_record — 新建特种车辆考试记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `special_exam_record` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员 user_id',
    `license_type` VARCHAR(10) NOT NULL COMMENT '车型 N1/N2/N3...',
    `subject` TINYINT NOT NULL COMMENT '科目: 1-理论, 2-实操',
    `score` TINYINT UNSIGNED DEFAULT NULL COMMENT '成绩 (0-100)',
    `pass_status` TINYINT DEFAULT NULL COMMENT '是否合格: 0-不合格, 1-合格',
    `file_id` INT UNSIGNED DEFAULT NULL COMMENT '关联文件ID（学员上传的成绩截图）',
    `retake_count` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该科目补考次数',
    `exam_date` DATETIME DEFAULT NULL COMMENT '考试时间',
    `cert_no` VARCHAR(50) DEFAULT NULL COMMENT '证书编号(双科通过后按规则生成)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_student_license` (`student_id`, `license_type`),
    KEY `idx_cert_no` (`cert_no`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特种车辆考试记录表';

-- ============================================
-- 6. config — 清理冗余配置
--    保留: cancel_advance_hours, max_no_show_count, no_show_punish_days
--    删除: hours_required_* (已在 license_config.required_hours 中管理)
-- ============================================
DELETE FROM `config` WHERE `config_key` LIKE 'hours_required_%';

-- 新增考试合格分数线配置（已有数据库补充）
INSERT IGNORE INTO `config` (`config_key`, `config_value`, `description`) VALUES
('exam_pass_score', '90', '考试合格分数线（百分制）');

-- ============================================
-- 脚本执行完毕
-- 验证: 查看新增/修改的表结构
-- ============================================
-- DESCRIBE license_config;
-- DESCRIBE exam_venue;
-- DESCRIBE exam_session;
-- DESCRIBE special_exam_record;
-- SELECT * FROM config;
