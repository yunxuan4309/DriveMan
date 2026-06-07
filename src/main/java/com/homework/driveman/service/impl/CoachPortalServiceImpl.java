package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.driveman.dto.ChangePasswordDTO;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.ExamSessionMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ICoachPortalService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.homework.driveman.dto.TimeSlotDTO;
import org.springframework.transaction.annotation.Transactional;
import com.homework.driveman.dto.CoachProfileUpdateDTO;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教练工作台业务实现
 * 包含工作量统计、评分查询等需要跨表聚合的复杂逻辑
 */
@Service
public class CoachPortalServiceImpl implements ICoachPortalService {

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> getStatistics(Integer coachId) {
        // 校验教练是否存在
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }

        // 1. 查询名下学员数（通过 student_coach 表统计正常绑定的学员）
        Long studentCount = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));

        // 2. 查询总学时
        BigDecimal totalHours = trainingRecordMapper.sumHoursByCoach(coachId);

        // 3. 查询各科目学时明细
        Map<Integer, BigDecimal> hoursBySubject = new HashMap<>();
        for (int subj = 1; subj <= 4; subj++) {
            BigDecimal h = trainingRecordMapper.sumHoursByCoachAndSubject(coachId, subj);
            hoursBySubject.put(subj, h);
        }

        // 4. 通过率计算：查出该教练名下所有学员的考试记录
        // 先查出该教练名下所有正常绑定的学员ID
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        List<Integer> studentIds = bindings.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toList());

        double passRate = 0.0;
        if (!studentIds.isEmpty()) {
            // 查询这些学员所有有成绩的考试记录
            List<ExamRegistration> exams = examRegistrationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRegistration>()
                            .in(ExamRegistration::getStudentId, studentIds)
                            .isNotNull(ExamRegistration::getPassStatus));
            long total = exams.size();
            long passed = exams.stream()
                    .filter(r -> r.getPassStatus() != null && r.getPassStatus() == 1)
                    .count();
            passRate = total > 0 ? (double) passed / total * 100 : 0.0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("studentCount", studentCount);
        result.put("totalTrainingHours", totalHours);
        result.put("hoursBySubject", hoursBySubject);
        result.put("examTotal", 0); // 下面覆盖
        result.put("examPassed", 0);
        result.put("passRate", String.format("%.1f%%", passRate));
        return result;
    }

    @Override
    public Map<String, Object> getRating(Integer coachId) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("rating", coach.getRating());
        result.put("coachYears", coach.getCoachYears());
        result.put("vehicleType", coach.getVehicleType());
        return result;
    }

    @Override
    public List<Map<String, Object>> getStudentExamRegistrations(Integer coachId) {
        // 校验教练是否存在
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }

        // 查出所有正常绑定的学员
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> studentIds = bindings.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toList());

        // 查这些学员的考试报名记录
        List<ExamRegistration> registrations = examRegistrationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRegistration>()
                        .in(ExamRegistration::getStudentId, studentIds)
                        .orderByDesc(ExamRegistration::getApplyTime));
        if (registrations.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量加载学员姓名
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 批量加载场次信息
        Set<Integer> sessionIds = registrations.stream()
                .map(ExamRegistration::getSessionId)
                .collect(Collectors.toSet());
        Map<Integer, ExamSession> sessionMap = examSessionMapper.selectBatchIds(sessionIds).stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s, (a, b) -> a));

        // 组装结果
        return registrations.stream().map(reg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reg.getId());
            map.put("studentId", reg.getStudentId());

            User student = userMap.get(reg.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);

            map.put("sessionId", reg.getSessionId());
            map.put("subject", reg.getSubject());
            map.put("status", reg.getStatus());

            // 状态中文描述
            String statusDesc = switch (reg.getStatus()) {
                case 0 -> "待审核";
                case 1 -> "审核通过";
                case 2 -> "审核不通过";
                case 3 -> "已考试";
                default -> "未知";
            };
            map.put("statusDesc", statusDesc);

            map.put("score", reg.getScore());
            map.put("passStatus", reg.getPassStatus());
            map.put("retakeCount", reg.getRetakeCount());
            map.put("isRetake", reg.getIsRetake());
            map.put("applyTime", reg.getApplyTime());
            map.put("auditTime", reg.getAuditTime());

            ExamSession session = sessionMap.get(reg.getSessionId());
            if (session != null) {
                map.put("examDate", session.getExamDate());
                map.put("startTime", session.getStartTime());
                map.put("location", session.getLocation());
                map.put("licenseType", session.getLicenseType());
            }

            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TimeSlotDTO> getTimeSlots(Integer coachId) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        String json = coach.getAvailableTime();
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            // 存储结构为 Map<String, List<String>>，如 {"monday":["09:00-12:00"]}
            Map<String, List<String>> map = objectMapper.readValue(json, new TypeReference<>() {});
            List<TimeSlotDTO> slots = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String day = entry.getKey();
                for (String range : entry.getValue()) {
                    String[] parts = range.split("-");
                    if (parts.length == 2) {
                        TimeSlotDTO dto = new TimeSlotDTO();
                        dto.setDayOfWeek(day);
                        dto.setStartTime(parts[0].trim());
                        dto.setEndTime(parts[1].trim());
                        slots.add(dto);
                    }
                }
            }
            return slots;
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "解析空闲时间失败");
        }
    }

    @Override
    public void setTimeSlots(Integer coachId, List<TimeSlotDTO> timeSlots) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        // 转换为 Map<String, List<String>>
        Map<String, List<String>> map = new HashMap<>();
        for (TimeSlotDTO slot : timeSlots) {
            String day = slot.getDayOfWeek().toLowerCase();
            String range = slot.getStartTime() + "-" + slot.getEndTime();
            map.computeIfAbsent(day, k -> new ArrayList<>()).add(range);
        }
        // 对每天的时间段排序（可选）
        for (List<String> ranges : map.values()) {
            ranges.sort(Comparator.naturalOrder());
        }
        try {
            String json = objectMapper.writeValueAsString(map);
            coach.setAvailableTime(json);
            coachMapper.updateById(coach);
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_INSERT, "保存空闲时间失败");
        }
    }

    @Override
    public void addTimeSlot(Integer coachId, TimeSlotDTO slot) {
        List<TimeSlotDTO> current = getTimeSlots(coachId);
        // 检查是否存在相同的时间段
        boolean exists = current.stream().anyMatch(s ->
                s.getDayOfWeek().equalsIgnoreCase(slot.getDayOfWeek()) &&
                        s.getStartTime().equals(slot.getStartTime()) &&
                        s.getEndTime().equals(slot.getEndTime()));
        if (exists) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该时间段已存在");
        }
        current.add(slot);
        setTimeSlots(coachId, current);
    }

    @Override
    public void removeTimeSlot(Integer coachId, TimeSlotDTO slot) {
        List<TimeSlotDTO> current = getTimeSlots(coachId);
        boolean removed = current.removeIf(s ->
                s.getDayOfWeek().equalsIgnoreCase(slot.getDayOfWeek()) &&
                        s.getStartTime().equals(slot.getStartTime()) &&
                        s.getEndTime().equals(slot.getEndTime()));
        if (!removed) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "未找到要删除的时间段");
        }
        setTimeSlots(coachId, current);
    }

    @Override
    public Map<String, Object> getProfile(Integer coachId) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        User user = userMapper.selectById(coach.getUserId());
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "关联用户不存在");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("coachId", coach.getCoachId());
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("idCard", user.getIdCard());
        profile.put("phone", user.getPhone());
        profile.put("address", user.getAddress());
        profile.put("avatar", user.getAvatar());
        profile.put("licenseType", user.getLicenseType());
        profile.put("coachYears", coach.getCoachYears());
        profile.put("rating", coach.getRating());
        profile.put("vehicleType", coach.getVehicleType());
        // profile.put("certificateUrl", coach.getCertificateUrl()); // 注释掉
        profile.put("availableTime", coach.getAvailableTime());
        return profile;
    }

    @Override
    @Transactional
    public void updateProfile(Integer coachId, CoachProfileUpdateDTO dto) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        User user = userMapper.selectById(coach.getUserId());
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "关联用户不存在");
        }

        // 更新 user 表字段
        boolean needUpdateUser = false;
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
            needUpdateUser = true;
        }
        if (dto.getPhone() != null) {
            // 手机号唯一性校验（排除自己）
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getPhone, dto.getPhone())
                            .ne(User::getUserId, user.getUserId()));
            if (count > 0) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT, "手机号已被其他用户使用");
            }
            user.setPhone(dto.getPhone());
            needUpdateUser = true;
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress());
            needUpdateUser = true;
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
            needUpdateUser = true;
        }
        if (needUpdateUser) {
            userMapper.updateById(user);
        }

        // 更新 coach 表字段
        boolean needUpdateCoach = false;
        if (dto.getCoachYears() != null) {
            coach.setCoachYears(dto.getCoachYears());
            needUpdateCoach = true;
        }
        // if (dto.getCertificateUrl() != null) { ... } // 注释掉
        if (needUpdateCoach) {
            coachMapper.updateById(coach);
        }
    }

    @Override
    @Transactional
    public void changePassword(Integer coachId, ChangePasswordDTO dto) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        User user = userMapper.selectById(coach.getUserId());
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "用户不存在");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "旧密码错误");
        }
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

}