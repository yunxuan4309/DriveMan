package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.PhysicalExam;

import java.util.List;
import java.util.Map;

/**
 * 体检申请服务接口
 */
public interface IPhysicalExamService extends IService<PhysicalExam> {

    /**
     * 学员提交体检申请
     */
    PhysicalExam apply(Integer studentId, Integer venueId, String examDate);

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
     * 获取可选的体检地点列表（从 venue 表查询 venue_type=3）
     */
    List<Map<String, Object>> getLocations();

    /**
     * 教练查看名下学员的体检申请
     */
    List<PhysicalExam> listByCoach(Integer userId);

    /**
     * 管理员分页查询体检申请，支持按学员姓名和状态筛选
     * @param page        分页参数
     * @param studentName 学员姓名关键词（可选）
     * @param status      状态（可选）
     */
    Page<Map<String, Object>> pageAll(Page<PhysicalExam> page, String studentName, Integer status);

    /**
     * 检查学员是否体检不合格（status=3 已完成 且 result=0 不合格），
     * 不合格则抛出 ServiceException 阻止后续操作
     */
    void checkPassed(Integer studentId);
}
