package com.homework.driveman.service;

import com.homework.driveman.entity.User;

/** PDF 生成服务 — 报名表、准考证 */
public interface IPdfService {

    /** 生成报名表 PDF，返回存储路径（相对 upload 根目录） */
    String generateRegistrationForm(User user);

    /** 生成准考证 PDF，返回存储路径（相对 upload 根目录） */
    String generateAdmissionTicket(User user);
}
