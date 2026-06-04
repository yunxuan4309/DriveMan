-- ============================================
-- 升级脚本：新增支付记录表
-- 说明: 收入统计、欠费管理的基础表
-- 执行: mysql -u root -proot driveman < upgrade_payment_record.sql
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
