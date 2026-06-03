package com.homework.driveman.service;

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
}