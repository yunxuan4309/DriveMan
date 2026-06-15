-- ============================================
-- 01 — 数据表结构
-- 说明: 全部 23 张表的 CREATE TABLE 语句
-- 前置: 数据库 driveman 已创建
-- 执行: mysql -u root -proot driveman < 01_schema.sql
-- ============================================

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
    `license_type` VARCHAR(10) DEFAULT NULL COMMENT '报考车型: C1/C2/N1...',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核,1-审核通过(已报名),2-审核不通过',
    `audit_reason` VARCHAR(200) DEFAULT NULL COMMENT '审核不通过原因',
    `license_obtained_date` DATETIME DEFAULT NULL COMMENT '驾照获取日期（当前车型全科通过时自动记录）',
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
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待确认,1-已确认,2-已拒绝,3-已取消',
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
    `license_type` VARCHAR(10) NOT NULL DEFAULT 'C1' COMMENT '培训车型: C1/C2/B1...',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_subject` (`student_id`, `subject_type`),
    KEY `idx_student_type_subject` (`student_id`, `license_type`, `subject_type`),
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
    `license_type` VARCHAR(10) NOT NULL DEFAULT 'C1' COMMENT '适用车型: C1/C2/B1...',
    `exam_date` DATE NOT NULL COMMENT '考试日期',
    `start_time` TIME DEFAULT NULL COMMENT '开始时间',
    `location` VARCHAR(200) NOT NULL COMMENT '考试地点',
    `venue_id` INT UNSIGNED DEFAULT NULL COMMENT '关联考场ID',
    `total_quota` INT UNSIGNED NOT NULL DEFAULT 50 COMMENT '总名额',
    `remaining_quota` INT UNSIGNED NOT NULL DEFAULT 50 COMMENT '剩余名额',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-报名中,2-已满,3-已结束',
    `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_subject` (`license_type`, `subject`),
    KEY `idx_subject_date` (`subject`, `exam_date`),
    KEY `idx_status` (`status`),
    KEY `idx_venue_id` (`venue_id`),
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
    `file_id` INT UNSIGNED DEFAULT NULL COMMENT '关联文件ID（学员上传的成绩截图）',
    `retake_count` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '补考次数',
    `is_retake` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否补考: 0-首次考试, 1-补考（仅标识记录，不影响计费；二次培训费走 retake_training_record）',
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
-- 7b. 二次培训记录表
-- 学员挂科后申请二次培训（补考培训）的流程记录。
-- 全包学员免缴费（is_free=1），非全包学员需缴纳培训费。
-- ============================================
CREATE TABLE IF NOT EXISTS `retake_training_record` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员user_id',
    `coach_id` INT UNSIGNED DEFAULT NULL COMMENT '教练user_id（培训指派的教练）',
    `exam_registration_id` INT UNSIGNED NOT NULL COMMENT '关联的挂科考试报名ID',
    `subject` TINYINT NOT NULL COMMENT '需培训的科目: 1-4',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-培训中, 2-已完成, 3-已取消',
    `is_free` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否免费: 1-全包学员免缴费, 0-需缴费',
    `amount` DECIMAL(8,2) DEFAULT NULL COMMENT '培训费金额(元)，非全包学员需缴纳',
    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '缴费状态: 0-无需缴费, 1-待缴费, 2-已缴费',
    `apply_reason` VARCHAR(200) DEFAULT NULL COMMENT '申请说明',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '培训完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_coach` (`coach_id`),
    KEY `idx_exam_reg` (`exam_registration_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二次培训记录表';

-- ============================================
-- 7c. 体检申请表
-- 学员提交体检申请，选择体检地点和时间，管理员审核并录入结果。
-- ============================================
CREATE TABLE IF NOT EXISTS `physical_exam` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员 user_id',
    `venue_id` INT UNSIGNED DEFAULT NULL COMMENT '关联场地ID（venue 表）',
    `license_type` VARCHAR(10) DEFAULT NULL COMMENT '关联车型（体检标准因车型而异）',
    `location` VARCHAR(200) NOT NULL COMMENT '体检地点（冗余显示字段，从 venue 表同步）',
    `exam_date` DATE NOT NULL COMMENT '预约体检日期',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-审核通过, 2-审核不通过, 3-已完成',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '审核备注（不通过原因等）',
    `file_id` INT UNSIGNED DEFAULT NULL COMMENT '关联文件ID（体检报告上传后回填）',
    `result` TINYINT DEFAULT NULL COMMENT '体检结果: 0-不合格, 1-合格, NULL-未出结果',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检申请表';

-- ============================================
-- 7d. 增驾申请表
-- 学员申请增驾（同级或升级），管理员审核并录入考试结果。
-- upgrade_type: 1-同级增驾, 2-升级增驾
-- ============================================
CREATE TABLE IF NOT EXISTS `license_upgrade` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员 user_id',
    `original_license` VARCHAR(10) NOT NULL COMMENT '原准驾车型',
    `target_license` VARCHAR(10) NOT NULL COMMENT '目标准驾车型',
    `upgrade_type` TINYINT NOT NULL COMMENT '增驾类型: 1-同级增驾, 2-升级增驾',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-审核通过, 2-审核不通过',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '审核备注',
    `exam_status` TINYINT DEFAULT 0 COMMENT '考试状态: 0-待考试, 1-考试通过, 2-考试不通过',
    `exam_remark` VARCHAR(200) DEFAULT NULL COMMENT '考试不通过原因/备注',
    `skip_subjects` VARCHAR(10) DEFAULT NULL COMMENT '跳过的科目编号(逗号分隔,如1,3)',
    `license_file_id` INT UNSIGNED DEFAULT NULL COMMENT '驾驶证材料文件ID（学员上传的驾驶证照片/扫描件）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='增驾申请表';

-- 7e. 残疾人信息表
-- 与 user 表一对一关联，用于 C5 驾照报名审核
CREATE TABLE IF NOT EXISTS `disability_info` (
    `id`                  INT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '残疾信息ID',
    `user_id`             INT UNSIGNED  NOT NULL COMMENT '关联用户ID',
    `disability_type`     TINYINT       NOT NULL COMMENT '残疾类型: 1-右下肢残疾, 2-双下肢残疾, 3-右手残疾, 4-听力障碍, 5-左手残疾, 9-其他',
    `certificate_no`      VARCHAR(50)   NOT NULL COMMENT '残疾人证号',
    `certificate_file_id` INT UNSIGNED  DEFAULT NULL COMMENT '残疾人证扫描件文件ID',
    `audit_status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-审核通过, 2-审核不通过',
    `audit_remark`        VARCHAR(200)  DEFAULT NULL COMMENT '审核备注',
    `audit_time`          DATETIME      DEFAULT NULL COMMENT '审核时间',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='残疾人信息表';

-- 7f. 特殊人群记录表（犯罪、酒驾、毒驾等）
-- 与 user 表关联，用于报名审核时的资格校验
CREATE TABLE IF NOT EXISTS `special_person_record` (
    `id`                  INT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`             INT UNSIGNED  NOT NULL COMMENT '关联用户ID',
    `record_type`         TINYINT       NOT NULL COMMENT '记录类型: 1-犯罪记录, 2-饮酒驾驶, 3-醉酒驾驶, 4-吸毒/毒驾, 5-交通肇事逃逸, 6-超速/超员构成犯罪',
    `record_date`         DATE          NOT NULL COMMENT '违法/犯罪日期',
    `ban_years`           INT           DEFAULT NULL COMMENT '禁驾年限（年），null表示终生禁驾',
    `ban_end_date`        DATE          DEFAULT NULL COMMENT '禁驾截止日期，null表示终生禁驾',
    `court_doc_no`        VARCHAR(100)  NOT NULL COMMENT '法律文书编号',
    `court_doc_file_id`   INT UNSIGNED  DEFAULT NULL COMMENT '法律文书扫描件文件ID',
    `audit_status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-审核通过, 2-审核不通过',
    `audit_remark`        VARCHAR(200)  DEFAULT NULL COMMENT '审核备注',
    `audit_time`          DATETIME      DEFAULT NULL COMMENT '审核时间',
    `audit_user_id`       INT UNSIGNED  DEFAULT NULL COMMENT '审核人ID',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_record_type` (`record_type`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特殊人群记录表';

-- ============================================
-- 8. 文件表
-- ============================================
CREATE TABLE `file` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `user_id` INT UNSIGNED NOT NULL COMMENT '上传者user_id（文件归属人）',
    `file_name` VARCHAR(200) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '存储路径（相对 upload 根目录）',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `file_type` VARCHAR(20) NOT NULL COMMENT '文件分类（旧字段，向前兼容）',
    `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型: user_profile/enrollment/exam_ticket/registration_form/training_record/physical_exam/license_upgrade/coach_qualification',
    `biz_id` INT UNSIGNED DEFAULT NULL COMMENT '业务记录ID',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
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
    `coach_id` INT UNSIGNED NOT NULL COMMENT '目标教练coach_id（学员申请/教练移交的接收教练）',
    `source_coach_id` INT UNSIGNED DEFAULT NULL COMMENT '发起移交的教练coach_id，NULL表示学员自主申请',
    `transfer_reason` VARCHAR(200) DEFAULT NULL COMMENT '教练移交原因（学员主动申请时为NULL）',
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
    KEY `idx_source_coach` (`source_coach_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练选择/移交申请表';

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
    `license_type` VARCHAR(10) NOT NULL COMMENT 'C1/C2/N1...',
    `subject` TINYINT DEFAULT NULL COMMENT '科目:1-4, NULL表示套餐总价',
    `amount` DECIMAL(8,2) NOT NULL COMMENT '金额(元)',
    `description` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_license_subject` (`license_type`, `subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用标准表';

-- ============================================
-- 13. 车型科目配置表
-- ============================================
CREATE TABLE IF NOT EXISTS `license_config` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `license_type` VARCHAR(10) NOT NULL COMMENT '车型: C1/C2/B1/N1...',
    `subject` TINYINT NOT NULL COMMENT '科目: 1-4(小汽车), 1=理论,2=实操(特种车)',
    `required_hours` DECIMAL(4,1) NOT NULL DEFAULT 0 COMMENT '要求学时',
    `exam_items` VARCHAR(500) DEFAULT NULL COMMENT '考试项目，逗号分隔',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '科目说明',
    `sort_order` TINYINT NOT NULL DEFAULT 0 COMMENT '排序',
    `exam_mode` TINYINT NOT NULL DEFAULT 1 COMMENT '考试模式: 1-小汽车(科一~科四), 2-特种车辆(理论+实操)',
    `coach_audit_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '报名考试是否需要教练审核: 1-需要, 0-不需要',
    `cert_name` VARCHAR(50) DEFAULT NULL COMMENT '获证名称(仅特种车辆使用, 如"叉车操作证")',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_subject` (`license_type`, `subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型科目配置表';

-- ============================================
-- 14. 场地统一管理表（考场/训练场地/体检地点）
-- ============================================
CREATE TABLE IF NOT EXISTS `venue` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场地ID',
    `venue_type` TINYINT NOT NULL COMMENT '类型: 1-考场, 2-训练场地, 3-体检地点',
    `name` VARCHAR(100) NOT NULL COMMENT '场地名称',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `capacity` INT UNSIGNED DEFAULT NULL COMMENT '容纳人数',
    `max_vehicles` TINYINT UNSIGNED DEFAULT NULL COMMENT '最大同时容纳车辆数（仅训练场地使用）',
    `supported_types` VARCHAR(50) DEFAULT NULL COMMENT '支持训练的车型，逗号分隔，NULL表示不限',
    `subjects` VARCHAR(20) DEFAULT NULL COMMENT '支持训练的科目，逗号分隔如"2,3"，NULL表示不限',
    `facilities` VARCHAR(500) DEFAULT NULL COMMENT '设施设备说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_venue_type` (`venue_type`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='场地统一管理表 — 考场/训练场地/体检地点';

-- ============================================
-- 15. 特种车辆考试记录表
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
-- 16. 教练准教车型变更申请表
-- ============================================
CREATE TABLE IF NOT EXISTS `coach_vehicle_application` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `coach_id` INT UNSIGNED NOT NULL COMMENT '教练 coach_id',
    `current_vehicle_type` VARCHAR(20) NOT NULL COMMENT '当前准教车型（申请时的快照）',
    `requested_vehicle_type` VARCHAR(20) NOT NULL COMMENT '申请的新准教车型',
    `apply_reason` VARCHAR(200) DEFAULT NULL COMMENT '申请理由',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
    `audit_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_coach_id` (`coach_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练准教车型变更申请表';

-- ============================================
-- 17. 支付记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员 user_id',
    `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型: registration_fee-报名费, exam_fee-考试费, familiarization_fee-合场费, other-其他',
    `biz_id` INT UNSIGNED DEFAULT NULL COMMENT '关联业务记录ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额(元)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已退款',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_biz_type` (`biz_type`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表 — 记录每笔应收/实收/退款';

-- ============================================
-- 18. 合场记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `familiarization_record` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员 user_id',
    `exam_session_id` INT UNSIGNED NOT NULL COMMENT '关联考试场次ID',
    `subject` TINYINT NOT NULL COMMENT '科目（冗余，取考场次科目）',
    `car_type` TINYINT NOT NULL COMMENT '用车类型: 1-教练车(教练陪同), 2-考试车(考场提供)',
    `coach_id` INT UNSIGNED DEFAULT NULL COMMENT '陪同教练 coach_id（教练车模式时必填）',
    `amount` DECIMAL(8,2) NOT NULL COMMENT '合场费用(元)',
    `payment_record_id` INT UNSIGNED DEFAULT NULL COMMENT '关联支付记录ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付(待安排), 2-已完成, 3-已取消',
    `scheduled_time` DATETIME DEFAULT NULL COMMENT '预约合场时间',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_exam_session_id` (`exam_session_id`),
    KEY `idx_coach_id` (`coach_id`),
    KEY `idx_status` (`status`),
    KEY `idx_payment_record_id` (`payment_record_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合场记录表 — 学员考前熟悉考场/考车';
