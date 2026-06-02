package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.vo.CoachRatingVO;
import com.homework.driveman.vo.CoachWorkloadVO;
import com.homework.driveman.vo.StudentInfoVO;

import java.util.List;

/** 教练业务接口 */
public interface ICoachService extends IService<Coach> {

    /**
     * 为学员推荐教练（推荐算法）
     * 匹配规则: 准教车型包含学员报考车型 → 按评分降序
     * @param licenseType 学员报考车型 (C1/C2/...)
     * @param topN        返回前 N 条
     * @return 推荐教练列表（只包含 vehicleType 匹配的）
     */
    List<Coach> recommend(String licenseType, int topN);
    /**
     * 查看名下学员列表
     */
    List<StudentInfoVO> getMyStudents(Integer coachId);

    /**
     * 设置教练空闲时间
     * @param coachId       教练ID
     * @param availableTime 空闲时间 JSON 字符串
     */
    void setAvailableTime(Integer coachId, String availableTime);

    /**
     * 获取教练工作量统计
     * @param coachId 教练ID
     * @return 工作量统计VO
     */
    CoachWorkloadVO getWorkload(Integer coachId);

    /**
     * 获取教练个人评分
     * @param coachId 教练ID
     * @return 评分VO
     */
    CoachRatingVO getRating(Integer coachId);
}
