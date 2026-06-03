package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练准教车型变更申请表
 * status: 0-待审核, 1-已通过, 2-已拒绝
 */
@Data
@TableName("coach_vehicle_application")
public class CoachVehicleApplication {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 教练 coach_id */
    private Integer coachId;

    /** 当前准教车型（申请时的快照） */
    private String currentVehicleType;

    /** 申请的新准教车型 */
    private String requestedVehicleType;

    /** 申请理由 */
    private String applyReason;

    /** 状态: 0-待审核, 1-已通过, 2-已拒绝 */
    private Integer status;

    /** 拒绝原因 */
    private String auditReason;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
