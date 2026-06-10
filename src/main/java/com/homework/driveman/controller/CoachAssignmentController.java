package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ICoachService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教练分配控制器 — 自动推荐 + 手动分配 + 查看/解绑
 */
@Tag(name = "教练分配")
@RestController
@RequestMapping("/coach-assignments")
public class CoachAssignmentController {

    @Autowired
    private ICoachService coachService;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoachMapper coachMapper;

    @RequireRole({1,3})
    @Operation(summary = "自动推荐教练",
            description = "根据学员报考车型匹配准教车型，按评分降序返回前 N 条")
    @GetMapping("/recommend")
    public JsonResult<List<Coach>> recommend(@RequestParam Integer studentId,
                                             @RequestParam(defaultValue = "5") int topN) {
        User student = userMapper.selectById(studentId);
        if (student == null || student.getLicenseType() == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在或未填写报考车型");
        }
        List<Coach> recommended = coachService.recommend(student.getLicenseType(), topN);
        return JsonResult.ok(recommended);
    }

    @RequireRole(3)
    @Operation(summary = "手动分配教练",
            description = "管理员直接为学员绑定一个教练，同时写入 student_coach 表")
    @Transactional
    @PostMapping
    public JsonResult<Void> assign(@RequestParam Integer studentId,
                                   @RequestParam Integer coachId) {
        // 校验是否已绑定
        Long count = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, studentId)
                        .eq(StudentCoach::getStatus, 1));
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该学员已绑定教练，请先解绑");
        }

        StudentCoach sc = new StudentCoach();
        sc.setStudentId(studentId);
        sc.setCoachId(coachId);
        sc.setBindTime(LocalDateTime.now());
        sc.setStatus(1);
        studentCoachMapper.insert(sc);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "分页查询绑定关系",
            description = "支持按学员姓名和教练姓名搜索，返回绑定记录含学员姓名和教练姓名")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) String studentName,
                                                          @RequestParam(required = false) String coachName) {
        LambdaQueryWrapper<StudentCoach> wrapper = new LambdaQueryWrapper<StudentCoach>()
                .eq(StudentCoach::getStatus, 1);

        if (studentName != null && !studentName.isEmpty()) {
            List<Integer> matchedStudentIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, studentName)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).toList();
            if (matchedStudentIds.isEmpty()) {
                return JsonResult.ok(new Page<>(page, size, 0));
            }
            wrapper.in(StudentCoach::getStudentId, matchedStudentIds);
        }

        if (coachName != null && !coachName.isEmpty()) {
            List<Integer> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, coachName)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).toList();
            if (matchedUserIds.isEmpty()) {
                return JsonResult.ok(new Page<>(page, size, 0));
            }
            List<Integer> matchedCoachIds = coachMapper.selectList(
                    new LambdaQueryWrapper<Coach>()
                            .in(Coach::getUserId, matchedUserIds)
                            .select(Coach::getCoachId)
            ).stream().map(Coach::getCoachId).toList();
            if (matchedCoachIds.isEmpty()) {
                return JsonResult.ok(new Page<>(page, size, 0));
            }
            wrapper.in(StudentCoach::getCoachId, matchedCoachIds);
        }

        Page<StudentCoach> scPage = studentCoachMapper.selectPage(new Page<>(page, size), wrapper);

        List<StudentCoach> records = scPage.getRecords();
        if (records.isEmpty()) {
            return JsonResult.ok(new Page<>(scPage.getCurrent(), scPage.getSize(), scPage.getTotal()));
        }

        Set<Integer> studentIds = records.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toSet());
        Map<Integer, User> studentMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        Set<Integer> coachIds = records.stream()
                .map(StudentCoach::getCoachId)
                .collect(Collectors.toSet());
        Map<Integer, Coach> coachObjMap = coachMapper.selectBatchIds(coachIds).stream()
                .collect(Collectors.toMap(Coach::getCoachId, c -> c, (a, b) -> a));
        Set<Integer> coachUserIds = coachObjMap.values().stream()
                .map(Coach::getUserId)
                .collect(Collectors.toSet());
        Map<Integer, User> coachUserMap = coachUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(coachUserIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        List<Map<String, Object>> result = records.stream().map(sc -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", sc.getId());
            map.put("studentId", sc.getStudentId());
            map.put("coachId", sc.getCoachId());
            map.put("bindTime", sc.getBindTime());

            User student = studentMap.get(sc.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : "未知");

            Coach coachObj = coachObjMap.get(sc.getCoachId());
            if (coachObj != null) {
                User coachUser = coachUserMap.get(coachObj.getUserId());
                map.put("coachName", coachUser != null ? coachUser.getRealName() : "未知");
            } else {
                map.put("coachName", "未知");
            }
            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(scPage.getCurrent(), scPage.getSize(), scPage.getTotal());
        resultPage.setRecords(result);
        return JsonResult.ok(resultPage);
    }

    @RequireRole(3)
    @Operation(summary = "查询指定教练名下的学员",
            description = "管理员按教练查看名下所有正常绑定的学员列表")
    @GetMapping("/coach/{coachId}/students")
    public JsonResult<Map<String, Object>> listStudentsByCoach(@PathVariable Integer coachId) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }
        User coachUser = userMapper.selectById(coach.getUserId());

        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));

        List<Map<String, Object>> students = bindings.stream().map(sc -> {
            Map<String, Object> item = new HashMap<>();
            item.put("bindId", sc.getId());
            item.put("studentId", sc.getStudentId());
            item.put("bindTime", sc.getBindTime());

            User student = userMapper.selectById(sc.getStudentId());
            if (student != null) {
                item.put("realName", student.getRealName());
                item.put("phone", student.getPhone());
                item.put("licenseType", student.getLicenseType());
                item.put("idCard", student.getIdCard());
            }
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("coachName", coachUser != null ? coachUser.getRealName() : "未知");
        result.put("studentCount", students.size());
        result.put("students", students);
        return JsonResult.ok(result);
    }

    @RequireRole(3)
    @Operation(summary = "解绑教练")
    @PutMapping("/{id}/unbind")
    public JsonResult<Void> unbind(@PathVariable Integer id) {
        StudentCoach sc = studentCoachMapper.selectById(id);
        if (sc == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "绑定记录不存在");
        }
        sc.setStatus(0);
        studentCoachMapper.updateById(sc);
        return JsonResult.ok();
    }

    @Operation(summary = "查看我的教练",
            description = "学员查询自己当前绑定的教练详细信息")
    @GetMapping("/my-coach/{studentId}")
    public JsonResult<Map<String, Object>> getMyCoach(@PathVariable Integer studentId) {
        // 查询当前绑定关系
        StudentCoach sc = studentCoachMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, studentId)
                        .eq(StudentCoach::getStatus, 1));

        if (sc == null) {
            return JsonResult.ok(null);
        }

        // 查询教练详细信息
        Coach coach = coachService.getById(sc.getCoachId());
        if (coach == null) {
            return JsonResult.ok(null);
        }

        // 查询教练对应的用户信息
        User coachUser = userMapper.selectById(coach.getUserId());

        // 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coach.getCoachId());
        result.put("userId", coach.getUserId());
        result.put("realName", coachUser != null ? coachUser.getRealName() : null);
        result.put("phone", coachUser != null ? coachUser.getPhone() : null);
        result.put("rating", coach.getRating());
        result.put("coachYears", coach.getCoachYears());
        result.put("vehicleType", coach.getVehicleType());
        result.put("availableTime", coach.getAvailableTime());
        result.put("bindTime", sc.getBindTime());

        return JsonResult.ok(result);
    }
}
