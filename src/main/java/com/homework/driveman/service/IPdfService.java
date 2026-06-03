package com.homework.driveman.service;

import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.User;

import java.math.BigDecimal;
import java.util.Map;

/** PDF 生成服务 — 报名表、准考证、培训记录表 */
public interface IPdfService {

    /** 生成报名表 PDF，返回存储路径（相对 upload 根目录） */
    String generateRegistrationForm(User user);

    /**
     * 生成准考证 PDF，返回存储路径（相对 upload 根目录）
     * @param session 考试场次（为 null 时生成通用准考证，不填具体场次信息）
     */
    String generateAdmissionTicket(User user, ExamSession session);

    /**
     * 生成培训记录表 PDF，返回存储路径（相对 upload 根目录）
     * @param student      学员信息
     * @param coachName    教练姓名
     * @param schoolName   驾校名称
     * @param hoursPerSubject 各科目累计学时 key=科目 (1/2/3/4), value=学时
     * @param licenseType  报考车型
     */
    String generateTrainingRecord(User student, String coachName, String schoolName,
                                  Map<Integer, BigDecimal> hoursPerSubject, String licenseType);
}
