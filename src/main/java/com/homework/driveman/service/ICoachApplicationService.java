package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.CoachApplication;

import java.util.Map;

/** 教练申请审核业务接口 */
public interface ICoachApplicationService extends IService<CoachApplication> {

    /**
     * 分页查询教练申请记录，附带学员姓名、目标教练姓名、源教练姓名
     * @param page        分页参数
     * @param status      可选，按状态筛选（0-待审核, 1-通过, 2-拒绝）
     * @param studentName 可选，学员姓名关键词搜索
     */
    Page<Map<String, Object>> pageWithDetails(Page<CoachApplication> page, Integer status, String studentName);
}
