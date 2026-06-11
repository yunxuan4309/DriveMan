package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.SpecialExamRecord;

import java.util.Map;

/** 特种车辆考试记录业务接口 */
public interface ISpecialExamRecordService extends IService<SpecialExamRecord> {

    /**
     * 分页查询特种车辆考试记录，支持多条件筛选
     * @param page        分页参数
     * @param studentName 学员姓名关键词（可选）
     * @param licenseType 车型（可选）
     * @param subject     科目（可选）
     * @param passStatus  是否合格（可选）
     */
    Page<Map<String, Object>> pageWithDetails(Page<SpecialExamRecord> page,
                                              String studentName,
                                              String licenseType,
                                              Integer subject,
                                              Integer passStatus);
}
