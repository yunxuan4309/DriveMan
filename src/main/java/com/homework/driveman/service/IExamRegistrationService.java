package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.ExamRegistration;

import java.util.Map;

/** 考试报名业务接口 */
public interface IExamRegistrationService extends IService<ExamRegistration> {

    /**
     * 分页查询考试报名记录，附带学员姓名和场次信息
     * @param status  可选，按状态筛选（0-待审核, 1-审核通过, 2-审核不通过, 3-已考试）
     * @param keyword 可选，学员姓名关键词模糊搜索
     */
    Page<Map<String, Object>> pageWithDetails(Page<ExamRegistration> page, Integer status, String keyword);
}
