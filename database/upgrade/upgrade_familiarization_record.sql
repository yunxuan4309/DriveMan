-- ============================================
-- 升级脚本：新增合场记录表
-- 说明: 学员考前熟悉考场/考车的费用与预约管理
-- 执行: mysql -u root -proot driveman < upgrade_familiarization_record.sql
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
