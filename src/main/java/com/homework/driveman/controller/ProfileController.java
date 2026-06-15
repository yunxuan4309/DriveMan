package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.*;
import com.homework.driveman.mapper.*;
import com.homework.driveman.service.IConfigService;
import com.homework.driveman.service.IProgressService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 个人信息控制器 — 学员/教练点击头像查看个人基本信息
 */
@Tag(name = "个人信息")
@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IProgressService progressService;

    @Autowired
    private IConfigService configService;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private FeeStandardMapper feeStandardMapper;

    @Autowired
    private PhysicalExamMapper physicalExamMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    @Operation(summary = "获取个人信息",
            description = "根据 JWT 中的角色自动返回学员或教练的个人信息。管理员暂不支持。")
    @GetMapping
    public JsonResult<Map<String, Object>> getProfile(HttpServletRequest request) {
        CurrentUser cu = getCurrentUser(request);
        if (cu.getRole() == 1) {
            return JsonResult.ok(buildStudentProfile(cu.getUserId()));
        } else if (cu.getRole() == 2) {
            return JsonResult.ok(buildCoachProfile(cu.getUserId()));
        }
        return JsonResult.ok(null);
    }

    // ==================== 学员个人信息 ====================

    private Map<String, Object> buildStudentProfile(Integer userId) {
        User student = userService.getById(userId);
        if (student == null) return Collections.emptyMap();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("role", 1);

        // 1. 基本信息
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("username", student.getUsername());
        basic.put("realName", student.getRealName());
        basic.put("phone", maskPhone(student.getPhone()));
        basic.put("idCard", maskIdCard(student.getIdCard()));
        basic.put("address", student.getAddress());
        basic.put("licenseType", student.getLicenseType());
        basic.put("avatar", student.getAvatar());
        basic.put("status", student.getStatus());
        basic.put("statusDesc", getStudentStatusDesc(student.getStatus()));
        basic.put("enrollDate", student.getCreateTime());
        basic.put("licenseObtainedDate", student.getLicenseObtainedDate());
        basic.put("existingLicense", student.getExistingLicense());
        basic.put("existingLicenseYears", student.getExistingLicenseYears());
        basic.put("existingLicenseFileId", student.getExistingLicenseFileId());
        profile.put("basic", basic);

        // 2. 报名套餐（subject IS NULL 有多条时取金额最大的=主套餐）
        List<FeeStandard> pkgs = feeStandardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FeeStandard>()
                        .eq(FeeStandard::getLicenseType, student.getLicenseType())
                        .isNull(FeeStandard::getSubject)
                        .orderByDesc(FeeStandard::getAmount)
                        .last("LIMIT 1"));
        Map<String, Object> enrollment = new LinkedHashMap<>();
        if (pkgs != null && !pkgs.isEmpty()) {
            FeeStandard pkg = pkgs.get(0);
            enrollment.put("packageName", pkg.getDescription());
            enrollment.put("packageAmount", pkg.getAmount());
            enrollment.put("isFullPackage", pkg.getDescription() != null && pkg.getDescription().contains("全包"));
        }
        profile.put("enrollment", enrollment);

        // 3. 绑定教练
        StudentCoach binding = studentCoachMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, userId)
                        .eq(StudentCoach::getStatus, 1));
        Map<String, Object> coachInfo = new LinkedHashMap<>();
        if (binding != null) {
            Coach coach = coachMapper.selectById(binding.getCoachId());
            if (coach != null) {
                User coachUser = userService.getById(coach.getUserId());
                coachInfo.put("coachName", coachUser != null ? coachUser.getRealName() : null);
                coachInfo.put("coachYears", coach.getCoachYears());
                coachInfo.put("coachRating", coach.getRating());
                coachInfo.put("coachVehicleType", coach.getVehicleType());
            }
            coachInfo.put("bindTime", binding.getBindTime());
        }
        profile.put("coach", coachInfo);

        // 4. 学习进度
        Map<String, Object> progress = new LinkedHashMap<>();
        try {
            Map<String, Object> p = progressService.getProgress(userId);
            progress.put("progressPercent", p.getOrDefault("progressPercent", 0));
            progress.put("allPassed", p.getOrDefault("allPassed", false));

            // 增驾进度透传
            if (p.containsKey("hasActiveUpgrade") && Boolean.TRUE.equals(p.get("hasActiveUpgrade"))) {
                progress.put("hasActiveUpgrade", true);
                progress.put("upgradeTargetLicense", p.get("upgradeTargetLicense"));
                progress.put("upgradeSkipSubjects", p.get("upgradeSkipSubjects"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subjects = (List<Map<String, Object>>) p.get("subjects");
            List<Map<String, Object>> passedList = new ArrayList<>();
            if (subjects != null) {
                for (Map<String, Object> s : subjects) {
                    if ("passed".equals(s.get("status")) || "skipped".equals(s.get("status"))) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("subject", s.get("subject"));
                        item.put("description", s.get("description"));
                        item.put("score", s.getOrDefault("score", 0));
                        item.put("status", s.get("status"));
                        passedList.add(item);
                    }
                }
            }
            progress.put("passedSubjects", passedList);
        } catch (Exception e) {
            progress.put("progressPercent", 0);
            progress.put("allPassed", false);
            progress.put("passedSubjects", Collections.emptyList());
        }
        profile.put("progress", progress);

        // 5. 体检状态
        PhysicalExam latestExam = physicalExamMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PhysicalExam>()
                        .eq(PhysicalExam::getStudentId, userId)
                        .eq(PhysicalExam::getLicenseType, student.getLicenseType())
                        .orderByDesc(PhysicalExam::getCreateTime)
                        .last("LIMIT 1"));
        Map<String, Object> physical = new LinkedHashMap<>();
        if (latestExam != null) {
            physical.put("status", latestExam.getStatus());
            physical.put("statusDesc", getPhysicalStatusDesc(latestExam.getStatus()));
            physical.put("result", latestExam.getResult());
            physical.put("resultDesc", latestExam.getResult() == null ? "未出结果"
                    : (latestExam.getResult() == 1 ? "合格" : "不合格"));
            physical.put("examDate", latestExam.getExamDate());
            physical.put("licenseType", latestExam.getLicenseType());
        }
        profile.put("physical", physical);

        // 6. 缴费概况
        // PaymentRecord.status: 0-待支付, 1-已支付, 2-已退款
        List<PaymentRecord> payments = paymentRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getStudentId, userId));
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalUnpaid = BigDecimal.ZERO;
        for (PaymentRecord pr : payments) {
            if (pr.getStatus() != null && pr.getStatus() == 1) {
                totalPaid = totalPaid.add(pr.getAmount());
            } else {
                totalUnpaid = totalUnpaid.add(pr.getAmount());
            }
        }
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("totalPaid", totalPaid);
        payment.put("totalUnpaid", totalUnpaid);
        payment.put("totalAmount", totalPaid.add(totalUnpaid));
        payment.put("paidCount", payments.stream().filter(p -> p.getStatus() != null && p.getStatus() == 1).count());
        payment.put("unpaidCount", payments.stream().filter(p -> p.getStatus() == null || p.getStatus() != 1).count());
        profile.put("payment", payment);

        return profile;
    }

    // ==================== 教练个人信息 ====================

    private Map<String, Object> buildCoachProfile(Integer userId) {
        User coachUser = userService.getById(userId);
        if (coachUser == null) return Collections.emptyMap();

        Coach coach = coachMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Coach>()
                        .eq(Coach::getUserId, userId));
        if (coach == null) return Collections.emptyMap();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("role", 2);

        // 1. 基本信息
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("username", coachUser.getUsername());
        basic.put("realName", coachUser.getRealName());
        basic.put("phone", maskPhone(coachUser.getPhone()));
        basic.put("idCard", maskIdCard(coachUser.getIdCard()));
        basic.put("address", coachUser.getAddress());
        basic.put("avatar", coachUser.getAvatar());
        basic.put("registerDate", coachUser.getCreateTime());
        basic.put("rating", coach.getRating());
        basic.put("coachYears", coach.getCoachYears());
        basic.put("vehicleType", coach.getVehicleType());
        profile.put("basic", basic);

        // 2. 教学概况
        Map<String, Object> stats = new LinkedHashMap<>();

        // 名下学员数
        Long studentCount = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coach.getCoachId())
                        .eq(StudentCoach::getStatus, 1));
        stats.put("studentCount", studentCount != null ? studentCount.intValue() : 0);

        // 累计教学学时
        BigDecimal totalHours = trainingRecordMapper.sumHoursByCoach(coach.getCoachId());
        stats.put("totalTrainingHours", totalHours != null ? totalHours : BigDecimal.ZERO);

        // 学员通过率
        List<Integer> examStudentIds = examRegistrationMapper.findExamStudentIds(coach.getCoachId());
        int totalExamStudents = examStudentIds != null ? examStudentIds.size() : 0;
        int passedAllCount = 0;
        if (totalExamStudents > 0) {
            for (Integer sid : examStudentIds) {
                Set<Integer> passed = examRegistrationMapper.findPassedSubjectsByStudent(sid, null);
                if (passed != null && passed.contains(1) && passed.contains(2)
                        && passed.contains(3) && passed.contains(4)) {
                    passedAllCount++;
                }
            }
        }
        double passRate = totalExamStudents == 0 ? 0.0
                : Math.round((passedAllCount * 10000.0 / totalExamStudents)) / 100.0;
        stats.put("passRate", passRate);
        stats.put("totalExamStudents", totalExamStudents);

        profile.put("stats", stats);

        return profile;
    }

    // ==================== 工具方法 ====================

    /** 手机号脱敏：前3后4保留，中间4位变* */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 身份证号脱敏：前6后4保留 */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) return idCard;
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    private String getStudentStatusDesc(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已报名";
            case 2 -> "审核不通过";
            default -> "未知";
        };
    }

    private String getPhysicalStatusDesc(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "审核不通过";
            case 3 -> "已完成";
            default -> "未知";
        };
    }
}
