package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.dto.RecordHoursDTO;
import com.homework.driveman.entity.TrainingRecord;

import java.math.BigDecimal;

/** 学时记录业务接口 */
public interface ITrainingRecordService extends IService<TrainingRecord> {
    BigDecimal getTotalHoursByStudentAndCoach(Integer studentId, Integer coachId);
    /**
     * 教练录入学时
     * @param coachId   教练ID
     * @param dto       录入学时参数
     */
    void recordTrainingHours(Integer coachId, RecordHoursDTO dto);
}
