package com.homework.driveman.service;

import com.homework.driveman.dto.ChangePasswordDTO;
import com.homework.driveman.dto.CoachProfileUpdateDTO;
import com.homework.driveman.dto.TimeSlotDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 教练工作台业务接口 — 包含教练端的统计和工作量查询
 */
public interface ICoachPortalService {

    /**
     * 获取教练的工作量统计数据
     * @param coachId 教练ID（coach表主键）
     * @return 包含学员数、总学时、通过率等统计信息
     */
    Map<String, Object> getStatistics(Integer coachId);

    /**
     * 获取教练的评分信息
     * @param coachId 教练ID
     * @return 评分数据
     */
    Map<String, Object> getRating(Integer coachId);

    /**
     * 获取教练名下学员的考试报名记录
     * @param coachId 教练ID（coach表主键）
     * @return 学员考试报名列表（含学员姓名、场次信息、审核状态）
     */
    List<Map<String, Object>> getStudentExamRegistrations(Integer coachId);

    /**
     * 获取教练的常规空闲时段列表（结构化，仅供学员参考）
     * @param coachId 教练ID
     * @return 时间段列表
     */
    List<TimeSlotDTO> getTimeSlots(Integer coachId);

    /**
     * 批量设置常规空闲时段（全量替换）
     * @param coachId 教练ID
     * @param timeSlots 新的时间段列表
     */
    void setTimeSlots(Integer coachId, List<TimeSlotDTO> timeSlots);

    /**
     * 添加一个常规空闲时段
     * @param coachId 教练ID
     * @param slot 时间段
     */
    void addTimeSlot(Integer coachId, TimeSlotDTO slot);

    /**
     * 删除一个常规空闲时段
     * @param coachId 教练ID
     * @param slot 要删除的时间段
     */
    void removeTimeSlot(Integer coachId, TimeSlotDTO slot);

    /**
     * 获取教练完整个人信息（包含用户信息和教练扩展信息）
     * @param coachId 教练ID
     * @return 个人信息Map
     */
    Map<String, Object> getProfile(Integer coachId);

    /**
     * 更新教练个人信息（白名单字段）
     * @param coachId 教练ID
     * @param dto 更新内容
     */
    void updateProfile(Integer coachId, CoachProfileUpdateDTO dto);

    /**
     * 修改教练登录密码
     * @param coachId 教练ID
     * @param dto 新旧密码
     */
    void changePassword(Integer coachId, ChangePasswordDTO dto);
}