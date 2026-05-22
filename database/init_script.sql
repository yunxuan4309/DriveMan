-- ============================================
-- 数据库名称: driveman
-- 说明: 驾校报名系统数据库（完整建表 + 初始化数据）
-- 创建日期: 2026-05-22
-- ============================================

-- 创建数据库
DROP DATABASE IF EXISTS `driveman`;
CREATE DATABASE IF NOT EXISTS `driveman`
CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE `driveman`;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE `user` (
    `user_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色: 1-学员,2-教练,3-管理员',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号(手机号/身份证号)',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `id_card` CHAR(18) NOT NULL COMMENT '身份证号',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '通讯地址',
    `license_type` VARCHAR(10) DEFAULT NULL COMMENT '报考车型: C1/C2/C5...',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核,1-审核通过(已报名),2-审核不通过',
    `audit_reason` VARCHAR(200) DEFAULT NULL COMMENT '审核不通过原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_id_card` (`id_card`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 教练扩展表
-- ============================================
CREATE TABLE `coach` (
    `coach_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教练ID',
    `user_id` INT UNSIGNED NOT NULL COMMENT '关联user.user_id',
    `rating` DECIMAL(2,1) NOT NULL DEFAULT 5.0 COMMENT '综合评分(1.0-5.0)',
    `available_time` JSON DEFAULT NULL COMMENT '空闲时间(JSON)',
    `coach_years` TINYINT UNSIGNED DEFAULT 0 COMMENT '执教年限',
    `vehicle_type` VARCHAR(20) DEFAULT NULL COMMENT '准教车型, 逗号分隔',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`coach_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_rating` (`rating`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='教练扩展表';

-- ============================================
-- 3. 学员-教练关联表
-- ============================================
CREATE TABLE `student_coach` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员user_id',
    `coach_id` INT UNSIGNED NOT NULL COMMENT '教练coach_id',
    `bind_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常绑定,0-已解绑',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_coach` (`coach_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='学员-教练关联表';

-- ============================================
-- 4. 约课表
-- ============================================
CREATE TABLE `appointment` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '约课ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员user_id',
    `coach_id` INT UNSIGNED NOT NULL COMMENT '教练coach_id',
    `start_time` DATETIME NOT NULL COMMENT '课程开始时间',
    `end_time` DATETIME NOT NULL COMMENT '课程结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待确认,1-已确认,2-已完成,3-已取消',
    `cancel_reason` VARCHAR(200) DEFAULT NULL COMMENT '取消原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_coach` (`coach_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_student_status_time` (`student_id`, `status`, `start_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='约课表';

-- ============================================
-- 5. 学时记录表
-- ============================================
CREATE TABLE `training_record` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员user_id',
    `coach_id` INT UNSIGNED NOT NULL COMMENT '教练coach_id',
    `appointment_id` INT UNSIGNED DEFAULT NULL COMMENT '关联的约课ID',
    `duration` DECIMAL(4,1) NOT NULL COMMENT '本次学时(小时)',
    `subject_type` TINYINT NOT NULL COMMENT '科目: 1-科目一,2-科目二,3-科目三,4-科目四',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_subject` (`student_id`, `subject_type`),
    KEY `idx_coach` (`coach_id`),
    KEY `idx_appointment` (`appointment_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='学时记录表';

-- ============================================
-- 6. 考试场次表
-- ============================================
CREATE TABLE `exam_session` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场次ID',
    `subject` TINYINT NOT NULL COMMENT '科目: 1-4',
    `exam_date` DATE NOT NULL COMMENT '考试日期',
    `start_time` TIME DEFAULT NULL COMMENT '开始时间',
    `location` VARCHAR(200) NOT NULL COMMENT '考试地点',
    `total_quota` INT UNSIGNED NOT NULL DEFAULT 50 COMMENT '总名额',
    `remaining_quota` INT UNSIGNED NOT NULL DEFAULT 50 COMMENT '剩余名额',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-报名中,2-已满,3-已结束',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_subject_date` (`subject`, `exam_date`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='考试场次表';

-- ============================================
-- 7. 考试报名表
-- ============================================
CREATE TABLE `exam_registration` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报名ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员user_id',
    `session_id` INT UNSIGNED NOT NULL COMMENT '考试场次ID',
    `subject` TINYINT NOT NULL COMMENT '科目(冗余字段)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核,1-审核通过,2-审核不通过,3-已考试',
    `score` TINYINT UNSIGNED DEFAULT NULL COMMENT '成绩(0-100)',
    `pass_status` TINYINT DEFAULT NULL COMMENT '是否合格: 0-不合格,1-合格',
    `retake_count` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '补考次数',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_subject_status` (`subject`, `status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='考试报名表';

-- ============================================
-- 8. 文件表
-- ============================================
CREATE TABLE `file` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `user_id` INT UNSIGNED NOT NULL COMMENT '上传者user_id',
    `file_name` VARCHAR(200) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_type` VARCHAR(20) NOT NULL COMMENT '文件类型: id_card_front, id_card_back, physical_exam, registration_pdf, admission_ticket',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ============================================
-- 9. 系统配置表
-- ============================================
CREATE TABLE `config` (
    `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 10. 教练选择申请表
-- ============================================
CREATE TABLE `coach_application` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员user_id',
    `coach_id` INT UNSIGNED NOT NULL COMMENT '申请的教练coach_id',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核,1-通过,2-拒绝',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `audit_time` DATETIME DEFAULT NULL,
    `audit_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_coach` (`coach_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练选择申请表';

-- ============================================
-- 11. 系统公告表
-- ============================================
CREATE TABLE IF NOT EXISTS `notice` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `content` TEXT NOT NULL,
    `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expire_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- ============================================
-- 12. 费用标准表
-- ============================================
CREATE TABLE IF NOT EXISTS `fee_standard` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `license_type` VARCHAR(10) NOT NULL COMMENT 'C1/C2',
    `subject` TINYINT DEFAULT NULL COMMENT '科目:1-4, NULL表示套餐总价',
    `amount` DECIMAL(8,2) NOT NULL COMMENT '金额(元)',
    `description` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_license_subject` (`license_type`, `subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用标准表';

-- ============================================
-- 初始化数据
-- ============================================

-- 1. 用户表 (密码均为 admin123)
INSERT IGNORE INTO `user` (`role`, `username`, `password`, `real_name`, `id_card`, `phone`, `address`, `license_type`, `status`) VALUES
(3, 'admin', '$2a$10$G6M7kH3lGH.FYWI3pMTwuuGwaDRHDWfbnR6530PeTL6ymSV.p29zS', '系统管理员', '11010119900307663X', '13800000000', '重庆市南岸区', NULL, 1),
(2, 'coach1', '$2a$10$G6M7kH3lGH.FYWI3pMTwuuGwaDRHDWfbnR6530PeTL6ymSV.p29zS', '张教练', '510101199505012345', '13812340001', '重庆市南岸区', 'C1', 1),
(2, 'coach2', '$2a$10$G6M7kH3lGH.FYWI3pMTwuuGwaDRHDWfbnR6530PeTL6ymSV.p29zS', '李教练', '510101198805026789', '13812340002', '重庆市南岸区', 'C1,C2', 1),
(1, 'student1', '$2a$10$G6M7kH3lGH.FYWI3pMTwuuGwaDRHDWfbnR6530PeTL6ymSV.p29zS', '王小明', '500101200001011234', '15912340001', '重庆市南岸区学府大道', 'C1', 1),
(1, 'student2', '$2a$10$G6M7kH3lGH.FYWI3pMTwuuGwaDRHDWfbnR6530PeTL6ymSV.p29zS', '李芳', '500101200105023456', '15912340002', '重庆市南岸区学府大道', 'C2', 1);

-- 2. 教练扩展表
INSERT IGNORE INTO `coach` (`user_id`, `rating`, `coach_years`, `vehicle_type`) VALUES
(2, 4.8, 6, 'C1'),
(3, 4.5, 10, 'C1,C2');

-- 3. 学员-教练关联
INSERT IGNORE INTO `student_coach` (`student_id`, `coach_id`, `status`) VALUES
(4, 1, 1),
(5, 2, 1);

-- 4. 约课记录
INSERT IGNORE INTO `appointment` (`student_id`, `coach_id`, `start_time`, `end_time`, `status`) VALUES
(4, 1, '2026-05-20 09:00:00', '2026-05-20 10:00:00', 1),
(5, 2, '2026-05-20 14:00:00', '2026-05-20 15:30:00', 0);

-- 5. 学时记录
INSERT IGNORE INTO `training_record` (`student_id`, `coach_id`, `appointment_id`, `duration`, `subject_type`) VALUES
(4, 1, 1, 1.0, 2),
(5, 2, 2, 1.5, 2);

-- 6. 考试场次
INSERT IGNORE INTO `exam_session` (`subject`, `exam_date`, `start_time`, `location`, `total_quota`, `remaining_quota`) VALUES
(1, '2026-06-10', '09:00:00', '南岸区车管所', 100, 98),
(2, '2026-06-15', '08:30:00', '南坪科目二考场', 80, 80),
(3, '2026-06-20', '13:00:00', '八公里科目三考场', 60, 60);

-- 7. 系统配置
INSERT IGNORE INTO `config` (`config_key`, `config_value`, `description`) VALUES
('hours_required_subject1', '12', '科目一要求学时'),
('hours_required_subject2_c1', '16', '科目二要求学时(C1)'),
('hours_required_subject2_c2', '12', '科目二要求学时(C2)'),
('hours_required_subject3', '24', '科目三要求学时'),
('cancel_advance_hours', '24', '取消约课需提前小时数'),
('max_no_show_count', '3', '爽约次数上限'),
('no_show_punish_days', '7', '爽约后禁止约课天数');

-- 8. 系统公告
INSERT IGNORE INTO `notice` (`title`, `content`, `publish_time`) VALUES
('系统上线通知', '驾校报名系统已正式上线，欢迎使用！如有问题请联系管理员。', NOW());

-- 9. 费用标准
INSERT IGNORE INTO `fee_standard` (`license_type`, `subject`, `amount`, `description`) VALUES
('C1', NULL, 3980.00, 'C1全包套餐（含补考费）'),
('C2', NULL, 4280.00, 'C2全包套餐（含补考费）'),
('C1', 2, 200.00, '科目二模拟训练费'),
('C2', 2, 200.00, '科目二模拟训练费');

-- ============================================
-- 脚本结束
-- ============================================
