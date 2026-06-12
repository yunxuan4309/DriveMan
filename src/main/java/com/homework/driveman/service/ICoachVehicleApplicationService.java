package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.CoachVehicleApplication;

import java.time.LocalDateTime;
import java.util.Map;

/** 教练准教车型变更业务接口 */
public interface ICoachVehicleApplicationService extends IService<CoachVehicleApplication> {

    /**
     * 教练提交准教车型变更申请
     * @param coachId             教练 coach_id
     * @param requestedVehicleType 申请的新准教车型
     * @param applyReason          申请理由
     */
    void submitApplication(Integer coachId, String requestedVehicleType, String applyReason);

    /**
     * 管理员审核申请
     * @param id         申请ID
     * @param approved   是否通过
     * @param auditReason 审核备注（拒绝时必填）
     */
    void audit(Integer id, boolean approved, String auditReason);

    /**
     * 分页查询待审核申请（含教练姓名），支持多条件搜索
     */
    Page<Map<String, Object>> pagePending(Page<?> page, String keyword, String currentVehicleType,
                                           String requestedVehicleType,
                                           LocalDateTime applyTimeStart, LocalDateTime applyTimeEnd);

    /**
     * 分页查询全部申请记录（含教练姓名），支持多条件搜索+审核时间范围
     */
    Page<Map<String, Object>> pageAll(Page<?> page, String keyword, String vehicleType,
                                       Integer status,
                                       LocalDateTime auditTimeStart, LocalDateTime auditTimeEnd);
}
