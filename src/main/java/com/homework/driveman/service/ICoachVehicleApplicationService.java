package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.CoachVehicleApplication;

import java.util.List;
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
     * 查询所有待审核申请（含教练名称）
     */
    List<Map<String, Object>> listPending();

    /**
     * 分页查询所有申请记录（含教练名称），支持按教练姓名、车型、状态筛选
     * @param page        分页参数
     * @param coachName   教练姓名关键词（可选）
     * @param vehicleType 车型（可选）
     * @param status      状态（可选）
     */
    Page<Map<String, Object>> listAll(Page<CoachVehicleApplication> page,
                                      String coachName,
                                      String vehicleType,
                                      Integer status);
}
