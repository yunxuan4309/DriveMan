package com.homework.driveman.controller;

import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
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
import java.util.List;
import java.util.Map;
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

    @Operation(summary = "查询所有绑定关系",
            description = "返回绑定记录，含学员姓名和教练姓名")
    @GetMapping
    public JsonResult<List<Map<String, Object>>> listAll() {
        List<StudentCoach> list = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStatus, 1));

        List<Map<String, Object>> result = list.stream().map(sc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sc.getId());
            map.put("studentId", sc.getStudentId());
            map.put("coachId", sc.getCoachId());
            map.put("bindTime", sc.getBindTime());

            User student = userMapper.selectById(sc.getStudentId());
            User coachUser = null;
            if (sc.getCoachId() != null) {
                Coach coach = coachService.getById(sc.getCoachId());
                if (coach != null) {
                    coachUser = userMapper.selectById(coach.getUserId());
                }
            }
            map.put("studentName", student != null ? student.getRealName() : "未知");
            map.put("coachName", coachUser != null ? coachUser.getRealName() : "未知");
            return map;
        }).collect(Collectors.toList());
        return JsonResult.ok(result);
    }

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
}
