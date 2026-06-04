package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.PhysicalExam;

import java.util.List;

/**
 * 体检申请服务接口
 */
public interface IPhysicalExamService extends IService<PhysicalExam> {

    /**
     * 学员提交体检申请
     */
    PhysicalExam apply(Integer studentId, String location, String examDate);

    /**
     * 查询学员的体检申请记录
     */
    List<PhysicalExam> listByStudent(Integer studentId);

    /**
     * 管理员审核体检申请
     */
    void audit(Integer id, Integer status, String remark);

    /**
     * 上传体检结果
     */
    void uploadResult(Integer id, Integer fileId, Integer result);

    /**
     * 教练查看名下学员的体检申请
     */
    List<PhysicalExam> listByCoach(Integer userId);
}
