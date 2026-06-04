package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.entity.RetakeTrainingRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 二次培训（补考培训）服务接口
 */
public interface IRetakeTrainingService {

    /**
     * 学员申请二次培训
     * @param studentId 学员ID
     * @param examRegistrationId 挂科的考试报名ID
     * @param coachId 希望指派的教练ID（可选）
     * @param reason 申请说明
     * @return 创建的记录
     */
    RetakeTrainingRecord apply(Integer studentId, Integer examRegistrationId, Integer coachId, String reason);

    /**
     * 管理员审核二次培训申请
     * @param id 记录ID
     * @param pass 是否通过
     * @param amount 培训费金额（非全包学员，为null则从config读取默认值）
     */
    void audit(Integer id, boolean pass, BigDecimal amount);

    /**
     * 完成培训（教练标记）
     */
    void complete(Integer id);

    /**
     * 取消申请
     */
    void cancel(Integer id);

    /**
     * 学员查询自己的二次培训记录
     */
    List<Map<String, Object>> listByStudent(Integer studentId);

    /**
     * 教练查询名下学员的二次培训记录
     */
    List<Map<String, Object>> listByCoach(Integer coachId);

    /**
     * 管理员分页查询所有二次培训记录
     */
    Page<Map<String, Object>> pageAll(Page<?> page);
}
