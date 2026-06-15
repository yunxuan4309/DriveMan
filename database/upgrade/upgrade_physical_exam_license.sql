-- ============================================
-- 体检申请表 + 增驾申请表
-- 说明: 新增 physical_exam（体检申请）和 license_upgrade（增驾申请）两张表
-- 前置: 数据库中已有 retake_training_record 表（或无）
-- 执行: mysql -u root -proot driveman < upgrade_physical_exam_license.sql
-- ============================================

-- ============================================
-- 7c. 体检申请表
-- 学员提交体检申请，选择体检地点和时间，管理员审核并录入结果。
-- ============================================
CREATE TABLE IF NOT EXISTS `physical_exam` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` INT UNSIGNED NOT NULL COMMENT '学员 user_id',
    `location` VARCHAR(200) NOT NULL COMMENT '体检地点',
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
