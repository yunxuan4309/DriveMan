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
     * @param studentId   学员ID
     * @param venueId     体检地点ID
     * @param examDate    预约日期 (yyyy-MM-dd)
     * @param licenseType 关联车型（体检标准因车型而异，默认使用学员当前车型）
     */
    PhysicalExam apply(Integer studentId, Integer venueId, String examDate, String licenseType);

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
     */
    Page<Map<String, Object>> pageAll(Page<PhysicalExam> page, String studentName, Integer status);

    /**
     * 检查学员是否存在"已完成的体检且结果为不合格"的记录，
     * 不合格则抛出 ServiceException 阻止后续操作（考试报名、增驾等）。
     */
    void checkPassed(Integer studentId);

    /**
     * 检查学员是否持有指定车型的体检合格记录（status=3, result=1）
     * 用于增驾场景：升级增驾需要目标车型的体检合格记录
     *
     * @param studentId   学员ID
     * @param licenseType 目标车型
     * @throws ServiceException 如果没有该车型的体检合格记录
     */
    void checkPassedForLicense(Integer studentId, String licenseType);
}
