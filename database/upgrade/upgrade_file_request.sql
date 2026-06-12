-- =====================================================
-- 文件提交请求表 — 管理员/教练向学员发起文件上传请求
-- 学员完成上传后自动标记为已完成
-- 执行方式: mysql -u root -proot driveman < database/upgrade/upgrade_file_request.sql
-- =====================================================

CREATE TABLE IF NOT EXISTS `file_request` (
    `id`          INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `requester_id` INT          NOT NULL COMMENT '发起人 user_id（管理员/教练）',
    `target_user_id` INT        NOT NULL COMMENT '目标用户 user_id（需要上传的人）',
    `title`        VARCHAR(100) NOT NULL COMMENT '请求标题，如"请上传体检结果报告"',
    `description`  VARCHAR(500) DEFAULT NULL COMMENT '详细说明',
    `biz_type`     VARCHAR(50)  DEFAULT NULL COMMENT '关联业务类型（可选，如 physical_exam）',
    `biz_id`       INT          DEFAULT NULL COMMENT '关联业务记录 ID（可选）',
    `file_type`    VARCHAR(50)  DEFAULT 'physical_exam_report' COMMENT '文件类型标识，上传时同步写入 file 表',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待提交, 1-已完成, 2-已取消',
    `remark`       VARCHAR(500) DEFAULT NULL COMMENT '内部备注',
    `deadline`     DATE         DEFAULT NULL COMMENT '截止日期（可选）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_target_user` (`target_user_id`, `status`),
    INDEX `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件提交请求表';
