package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.CoachSchedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 排班管理业务接口
 */
public interface ICoachScheduleService extends IService<CoachSchedule> {

    /**
     * 教练提交排班申请（含冲突检测）
     * @param schedule 排班信息
     */
    void apply(CoachSchedule schedule);

    /**
     * 管理员审核排班（通过/拒绝）
     * @param scheduleId 排班ID
     * @param status     1-通过, 2-拒绝
     * @param remark     审核备注
     */
    void audit(Integer scheduleId, Integer status, String remark);

    /**
     * 教练取消排班申请
     * @param scheduleId 排班ID
     * @param coachId    教练ID（校验归属）
     */
    void cancel(Integer scheduleId, Integer coachId);

    /**
     * 查询教练已通过的可约时段（学员端调用，自动查绑定教练）
     * @param studentId   学员 user_id
     * @param licenseType 可选：按车型筛选
     * @return 可约的排班列表
     */
    List<CoachSchedule> listAvailableForStudent(Integer studentId, String licenseType);

    /**
     * 分页+多条件搜索排班（管理员端）
     * 返回含教练姓名、车牌号、场地名称的 Map
     */
    Page<Map<String, Object>> pageSearch(Page<?> page, String keyword, String plateNumber,
                                          String venueName, String licenseType, Integer status,
                                          LocalDateTime startDateStart, LocalDateTime startDateEnd);
}
