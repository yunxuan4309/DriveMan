-- ============================================
-- 车型配置升级脚本（对已有数据库执行）
-- 说明：给已有数据库增加车型支持，不丢数据
-- ============================================

USE driveman;

-- 1. exam_session 表加字段
ALTER TABLE `exam_session`
ADD COLUMN `license_type` VARCHAR(10) NOT NULL DEFAULT 'C1' COMMENT '适用车型: C1/C2/B1...'
AFTER `subject`;

ALTER TABLE `exam_session`
ADD KEY `idx_type_subject` (`license_type`, `subject`);

-- 将已有场次默认设为 C1
UPDATE `exam_session` SET `license_type` = 'C1' WHERE `license_type` IS NULL;

-- 2. training_record 表加字段
ALTER TABLE `training_record`
ADD COLUMN `license_type` VARCHAR(10) NOT NULL DEFAULT 'C1' COMMENT '培训车型: C1/C2/B1...'
AFTER `subject_type`;

ALTER TABLE `training_record`
ADD KEY `idx_student_type_subject` (`student_id`, `license_type`, `subject_type`);

-- 将已有学时记录默认设为 C1
UPDATE `training_record` SET `license_type` = 'C1' WHERE `license_type` IS NULL;

-- 3. 新建车型科目配置表
CREATE TABLE IF NOT EXISTS `license_config` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `license_type` VARCHAR(10) NOT NULL COMMENT '车型: C1/C2/B1...',
    `subject` TINYINT NOT NULL COMMENT '科目: 1-4',
    `required_hours` DECIMAL(4,1) NOT NULL DEFAULT 0 COMMENT '要求学时',
    `exam_items` VARCHAR(500) DEFAULT NULL COMMENT '考试项目，逗号分隔',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '科目说明',
    `sort_order` TINYINT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_subject` (`license_type`, `subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型科目配置表';

-- 4. 插入车型配置数据
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`) VALUES
('C1', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1),
('C1', 2, 16,    '倒车入库,侧方停车,坡道定点停车和起步,直角转弯,曲线行驶',                                                    '科目二 场地驾驶技能', 2),
('C1', 3, 24,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头,夜间行驶',                                                       '科目三 道路驾驶技能', 3),
('C1', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4),

('C2', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1),
('C2', 2, 12,    '倒车入库,侧方停车,直角转弯,曲线行驶',                                                                  '科目二 场地驾驶技能（无坡道起步）', 2),
('C2', 3, 22,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头',                                                             '科目三 道路驾驶技能（无夜间行驶）', 3),
('C2', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4),

('B1', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1),
('B1', 2, 20,    '倒车入库,侧方停车,坡道定点停车和起步,直角转弯,曲线行驶,通过单边桥,通过限宽门',                                          '科目二 场地驾驶技能', 2),
('B1', 3, 30,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头,夜间行驶,模拟山区公路',                                                    '科目三 道路驾驶技能', 3),
('B1', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4);
