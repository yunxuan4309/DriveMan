-- ============================================
-- 二次培训（补考培训）流程支持（增量升级）
-- ============================================

-- 1. exam_registration 增加是否补考标记（仅记录标识，不参与计费）
ALTER TABLE `exam_registration`
    ADD COLUMN `is_retake` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否补考: 0-首次考试, 1-补考（仅标识，不影响计费）'
    AFTER `retake_count`;

-- 2. 新建二次培训记录表
--    学员挂科后申请二次培训，全包学员免缴费，非全包学员需缴纳培训费
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二次培训记录表 — 学员挂科后申请二次培训的流程记录';
