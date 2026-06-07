package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.Appointment;

/** 约课业务接口 */
public interface IAppointmentService extends IService<Appointment> {
    /**
     * 确认约课
     * @param appointmentId 约课ID
     * @param coachId       教练ID（用于校验归属）
     */
    void confirmAppointment(Integer appointmentId, Integer coachId);

    /**
     * 拒绝约课
     * @param appointmentId 约课ID
     * @param coachId       教练ID（用于校验归属）
     * @param reason        拒绝原因
     */
    void rejectAppointment(Integer appointmentId, Integer coachId, String reason);

    /**
     * 完成约课（约课上完后标记为已完成）
     * @param appointmentId 约课ID
     * @param coachId       教练ID（用于校验归属）
     */
    void completeAppointment(Integer appointmentId, Integer coachId);
}
