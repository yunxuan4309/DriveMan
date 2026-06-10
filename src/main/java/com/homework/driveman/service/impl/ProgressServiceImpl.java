package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.service.IProgressService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学员进度查询实现
 * 算法：按车型配置逐科目推算状态
 *   locked   → 上一科未通过
 *   learning → 已有学时但未达要求
 *   ready    → 学时已达标，可以报名考试
 *   passed   → 考试已通过
 */
@Service
public class ProgressServiceImpl implements IProgressService {

    @Autowired
    private IUserService userService;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Override
    public Map<String, Object> getProgress(Integer studentId) {
        // 1. 查出学员信息和报考车型
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在");
        }
        String type = student.getLicenseType();
        if (type == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该学员未填写报考车型");
        }

        // 2. 查出该车型的科目配置（按 sort_order 排序）
        List<LicenseConfig> configs = licenseConfigMapper.selectList(
                new LambdaQueryWrapper<LicenseConfig>()
                        .eq(LicenseConfig::getLicenseType, type)
                        .orderByAsc(LicenseConfig::getSortOrder));

        if (configs.isEmpty()) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND,
                    "未找到车型 " + type + " 的科目配置");
        }

        // 确定考试模式（同一车型所有科目共享一个 examMode）
        Integer examMode = configs.get(0).getExamMode();
        String certName = configs.get(0).getCertName();

        // 3. 查该学员所有科目的考试通过情况
        List<ExamRegistration> examResults = examRegistrationMapper.selectList(
                new LambdaQueryWrapper<ExamRegistration>()
                        .eq(ExamRegistration::getStudentId, studentId)
                        .isNotNull(ExamRegistration::getPassStatus)
                        .select(ExamRegistration::getSubject, ExamRegistration::getPassStatus,
                                ExamRegistration::getScore));
        Set<Integer> passedSubjects = examResults.stream()
                .filter(r -> r.getPassStatus() != null && r.getPassStatus() == 1)
                .map(ExamRegistration::getSubject)
                .collect(Collectors.toSet());
        Map<Integer, Integer> scoreMap = examResults.stream()
                .filter(r -> r.getPassStatus() != null && r.getPassStatus() == 1)
                .collect(Collectors.toMap(ExamRegistration::getSubject,
                        r -> r.getScore() != null ? r.getScore() : 0,
                        (a, b) -> b));

        // 4. 逐科目计算状态
        boolean prevPassed = true; // 科目一之前视为"已通过"（无前置条件）
        List<Map<String, Object>> subjects = new ArrayList<>();

        for (int i = 0; i < configs.size(); i++) {
            LicenseConfig cfg = configs.get(i);
            int subject = cfg.getSubject();

            // 查该科目学时
            BigDecimal trained = BigDecimal.ZERO;
            if (cfg.getRequiredHours().compareTo(BigDecimal.ZERO) > 0) {
                trained = trainingRecordMapper.sumTrainingHours(studentId, type, subject);
            }

            // 判断状态
            String status;
            Map<String, Object> subjectData = new LinkedHashMap<>();
            subjectData.put("subject", subject);
            subjectData.put("examMode", cfg.getExamMode());
            subjectData.put("description", cfg.getDescription());
            subjectData.put("requiredHours", cfg.getRequiredHours());
            subjectData.put("trainedHours", trained);

            // 考试项目
            if (cfg.getExamItems() != null && !cfg.getExamItems().isEmpty()) {
                subjectData.put("examItems", Arrays.asList(cfg.getExamItems().split(",")));
            } else {
                subjectData.put("examItems", null);
            }

            if (passedSubjects.contains(subject)) {
                status = "passed";
                subjectData.put("score", scoreMap.getOrDefault(subject, 0));
                prevPassed = true;
            } else if (!prevPassed) {
                status = "locked";
            } else if (cfg.getRequiredHours().compareTo(BigDecimal.ZERO) == 0) {
                // 无学时要求的科目（科目一/四），随时可考
                status = "ready";
            } else if (trained.compareTo(cfg.getRequiredHours()) >= 0) {
                status = "ready";
            } else {
                status = "learning";
            }

            subjectData.put("status", status);

            BigDecimal remaining = cfg.getRequiredHours().subtract(trained);
            subjectData.put("remainingHours", remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);

            subjects.add(subjectData);

            // 记录前置科目是否通过（用于下一科的 locked 判断）
            if (cfg.getRequiredHours().compareTo(BigDecimal.ZERO) == 0 && !passedSubjects.contains(subject)) {
                prevPassed = false;
            }
        }

        // 5. 组装最终结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("realName", student.getRealName());
        result.put("licenseType", type);
        result.put("examMode", examMode);
        result.put("certName", certName);
        result.put("subjects", subjects);

        // 统计总体进度
        long total = configs.size();
        long passed = passedSubjects.stream().filter(s ->
                configs.stream().anyMatch(c -> c.getSubject() == s)).count();
        result.put("progressPercent", total > 0 ? (int) (passed * 100 / total) : 0);

        // 判断是否全部科目已通过
        boolean allPassed = configs.stream()
                .allMatch(c -> passedSubjects.contains(c.getSubject()));
        result.put("allPassed", allPassed);

        // 特种车辆双科通过且有证书名称 → 可结业领证
        if (examMode != null && examMode == 2 && allPassed && certName != null) {
            result.put("certificate", certName);
        }

        return result;
    }
}
