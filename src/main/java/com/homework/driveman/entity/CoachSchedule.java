package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练排班/车辆使用申请表
 * status: 0-待审核, 1-已通过, 2-已拒绝, 3-已完成, 4-已取消
 */
@Data
@TableName("coach_schedule")
public class CoachSchedule {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 教练 coach_id */
    private Integer coachId;

    /** 车辆ID */
    private Integer vehicleId;

    /** 训练场地ID */
    private Integer venueId;

    /** 培训车型 */
    private String licenseType;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 该时段最大可容纳学员数 */
    private Integer maxStudents;

    /** 已预约学员数 */
    private Integer bookedCount;

    /** 状态: 0-待审核, 1-已通过, 2-已拒绝, 3-已完成, 4-已取消 */
    private Integer status;

    /** 申请说明 */
    private String applyReason;

    /** 审核备注/拒绝原因 */
    private String auditRemark;

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
