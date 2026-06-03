package com.homework.driveman.service.impl;

import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IPdfService;
import com.homework.driveman.web.ServiceCode;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * PDF 生成实现 — 基于 iText 7
 * 使用 Windows 系统字体 "Microsoft YaHei" 支持中文
 */
@Slf4j
@Service
public class PdfServiceImpl implements IPdfService {

    @Value("${drive.upload.path:./upload-files}")
    private String uploadPath;

    /** 尝试加载中文字体，优先使用微软雅黑 */
    private PdfFont getChineseFont() {
        String[] candidates = {
                "C:/Windows/Fonts/msyh.ttc",           // 微软雅黑
                "C:/Windows/Fonts/simsun.ttc",          // 宋体
                "C:/Windows/Fonts/simhei.ttf"           // 黑体
        };
        for (String path : candidates) {
            try {
                // .ttc 文件需要指定索引，通常为 0
                String fontPath = path.endsWith(".ttc") ? path + ",0" : path;
                return PdfFontFactory.createFont(fontPath, "Identity-H",
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception ignored) {
            }
        }
        throw new ServiceException(ServiceCode.ERROR_INSERT, "未找到中文字体，无法生成PDF");
    }

    @Override
    public String generateRegistrationForm(User user) {
        String relativePath = "registration_pdf/" + user.getUserId() + "_registration_" + LocalDate.now() + ".pdf";
        Path targetPath = Paths.get(uploadPath, relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_INSERT, "创建PDF目录失败");
        }

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
             PdfDocument pdf = new PdfDocument(new PdfWriter(fos));
             Document doc = new Document(pdf, PageSize.A4)) {

            PdfFont font = getChineseFont();
            doc.setFont(font);
            doc.setFontSize(12);

            // 标题
            Paragraph title = new Paragraph("驾校学员报名表")
                    .setFontSize(22).setBold()
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginBottom(30);
            doc.add(title);

            // 编号与日期
            doc.add(new Paragraph("编号: " + user.getUserId())
                    .setMarginBottom(5));
            doc.add(new Paragraph("日期: " + LocalDate.now())
                    .setMarginBottom(20));

            // 基本信息表格
            float[] colWidths = {120, 200, 120, 200};
            Table table = new Table(UnitValue.createPercentArray(colWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            addRow(table, "姓名", user.getRealName(), "性别", inferGender(user.getIdCard()));
            addRow(table, "身份证号", user.getIdCard(), "手机号", user.getPhone());
            addRow(table, "报考车型", user.getLicenseType(), "通讯地址", user.getAddress());
            addRow(table, "账号", user.getUsername(), "审核状态", "已通过");

            doc.add(table);

            // 底部说明
            Paragraph footer = new Paragraph("\n\n本人确认以上信息真实有效。")
                    .setMarginTop(40);
            doc.add(footer);
            doc.add(new Paragraph("学员签名: ____________    日期: ____________"));

            log.info("报名表PDF生成成功: {}", targetPath);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("报名表PDF生成失败", e);
            throw new ServiceException(ServiceCode.ERROR_INSERT, "报名表PDF生成失败");
        }
        return relativePath.replace("\\", "/");
    }

    @Override
    public String generateTrainingRecord(User student, String coachName, String schoolName,
                                          Map<Integer, BigDecimal> hoursPerSubject, String licenseType) {
        String relativePath = "training_record/" + student.getUserId() + "_record_" + LocalDate.now() + ".pdf";
        Path targetPath = Paths.get(uploadPath, relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_INSERT, "创建PDF目录失败");
        }

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
             PdfDocument pdf = new PdfDocument(new PdfWriter(fos));
             Document doc = new Document(pdf, PageSize.A4)) {

            PdfFont font = getChineseFont();
            doc.setFont(font);
            doc.setFontSize(12);

            // 标题
            Paragraph title = new Paragraph("机动车驾驶培训记录")
                    .setFontSize(22).setBold()
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginBottom(30);
            doc.add(title);

            // 驾校名称
            doc.add(new Paragraph("驾校名称: " + (schoolName != null ? schoolName : ""))
                    .setMarginBottom(5));
            doc.add(new Paragraph("培训车型: " + (licenseType != null ? licenseType : ""))
                    .setMarginBottom(20));

            // 学员信息
            float[] colWidths = {120, 200, 120, 200};
            Table infoTable = new Table(UnitValue.createPercentArray(colWidths));
            infoTable.setWidth(UnitValue.createPercentValue(100));

            addRow(infoTable, "姓名", student.getRealName(), "性别", inferGender(student.getIdCard()));
            addRow(infoTable, "身份证号", student.getIdCard(), "手机号", student.getPhone());
            addRow(infoTable, "教练姓名", coachName != null ? coachName : "", "培训车型", licenseType != null ? licenseType : "");

            doc.add(infoTable);

            // 学时汇总表
            Paragraph hoursTitle = new Paragraph("\n\n学时汇总")
                    .setBold().setFontSize(14).setMarginBottom(10);
            doc.add(hoursTitle);

            float[] hourCols = {100, 200, 100};
            Table hourTable = new Table(UnitValue.createPercentArray(hourCols));
            hourTable.setWidth(UnitValue.createPercentValue(100));

            // 表头
            hourTable.addHeaderCell(new Cell().add(new Paragraph("科目").setBold()));
            hourTable.addHeaderCell(new Cell().add(new Paragraph("科目名称").setBold()));
            hourTable.addHeaderCell(new Cell().add(new Paragraph("累计学时").setBold()));

            // 按科目 1-4 顺序显示
            String[] subjectNames = {"", "科目一", "科目二", "科目三", "科目四"};
            for (int i = 1; i <= 4; i++) {
                BigDecimal hours = hoursPerSubject.getOrDefault(i, BigDecimal.ZERO);
                String subjectName = i < subjectNames.length ? subjectNames[i] : "科目" + i;
                hourTable.addCell(new Cell().add(new Paragraph(String.valueOf(i))));
                hourTable.addCell(new Cell().add(new Paragraph(subjectName)));
                hourTable.addCell(new Cell().add(new Paragraph(hours.toString() + " 小时")));
            }
            doc.add(hourTable);

            // 底部信息
            doc.add(new Paragraph("\n\n\n"));
            doc.add(new Paragraph("教练签名: ____________    日期: ____________")
                    .setMarginBottom(5));
            doc.add(new Paragraph("驾校盖章: ____________    日期: ____________")
                    .setMarginBottom(5));
            doc.add(new Paragraph("\n生成日期: " + LocalDate.now()));

            log.info("培训记录表PDF生成成功: {}", targetPath);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("培训记录表PDF生成失败", e);
            throw new ServiceException(ServiceCode.ERROR_INSERT, "培训记录表PDF生成失败");
        }
        return relativePath.replace("\\", "/");
    }

    @Override
    public String generateAdmissionTicket(User user, ExamSession session) {
        String suffix = (session != null ? "_session_" + session.getId() : "");
        String relativePath = "admission_ticket/" + user.getUserId() + "_ticket" + suffix + "_" + LocalDate.now() + ".pdf";
        Path targetPath = Paths.get(uploadPath, relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_INSERT, "创建PDF目录失败");
        }

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
             PdfDocument pdf = new PdfDocument(new PdfWriter(fos));
             Document doc = new Document(pdf, PageSize.A4)) {

            PdfFont font = getChineseFont();
            doc.setFont(font);
            doc.setFontSize(12);

            // 标题
            Paragraph title = new Paragraph("准 考 证")
                    .setFontSize(24).setBold()
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginBottom(30);
            doc.add(title);

            // 考生信息
            float[] colWidths = {120, 200, 120, 200};
            Table table = new Table(UnitValue.createPercentArray(colWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            addRow(table, "姓名", user.getRealName(), "性别", inferGender(user.getIdCard()));
            addRow(table, "身份证号", user.getIdCard(), "报考车型", user.getLicenseType());

            if (session != null) {
                addRow(table, "考试日期", session.getExamDate().toString(),
                        "考试时间", session.getStartTime() != null ? session.getStartTime().toString() : "");
                addRow(table, "考试地点", session.getLocation(), "科目", "科目" + session.getSubject());
            } else {
                addRow(table, "考试日期", "见考试场次安排", "考试地点", "见考试场次安排");
            }

            doc.add(table);

            // 注意事项
            Paragraph notice = new Paragraph("\n\n注意事项:")
                    .setBold().setFontSize(14);
            doc.add(notice);
            doc.add(new Paragraph("1. 请携带本人身份证和本准考证参加考试。"));
            doc.add(new Paragraph("2. 请提前 30 分钟到达考场。"));
            doc.add(new Paragraph("3. 考试期间请遵守考场纪律。"));
            doc.add(new Paragraph("4. 本准考证盖章有效。"));

            // 盖章区
            Paragraph seal = new Paragraph("\n\n\n准考证盖章区")
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            doc.add(seal);

            log.info("准考证PDF生成成功: {}", targetPath);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("准考证PDF生成失败", e);
            throw new ServiceException(ServiceCode.ERROR_INSERT, "准考证PDF生成失败");
        }
        return relativePath.replace("\\", "/");
    }

    private void addRow(Table table, String label1, String value1, String label2, String value2) {
        table.addCell(new Cell().add(new Paragraph(label1)).setBold());
        table.addCell(new Cell().add(new Paragraph(value1 != null ? value1 : "")));
        table.addCell(new Cell().add(new Paragraph(label2)).setBold());
        table.addCell(new Cell().add(new Paragraph(value2 != null ? value2 : "")));
    }

    /** 根据身份证号第 17 位推断性别 */
    private String inferGender(String idCard) {
        if (idCard == null || idCard.length() < 18) return "未知";
        int digit = Integer.parseInt(idCard.charAt(16) + "");
        return (digit % 2 == 0) ? "女" : "男";
    }
}
