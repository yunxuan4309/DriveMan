package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ICoachService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教练管理控制器 — 教练信息的 CRUD 接口
 */
@Tag(name = "教练管理")
@RestController
@RequestMapping("/coaches")
public class CoachController {

    @Autowired
    private ICoachService coachService;

    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "分页查询教练",
            description = "返回教练信息及关联的用户真实姓名、用户名、手机号；支持按用户名、姓名模糊搜索、准驾车型、评分范围、教龄范围筛选")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(required = false) String username,
                                                      @RequestParam(required = false) String realName,
                                                      @RequestParam(required = false) String vehicleType,
                                                      @RequestParam(required = false) Integer ratingMin,
                                                      @RequestParam(required = false) Integer ratingMax,
                                                      @RequestParam(required = false) Integer coachYearsMin,
                                                      @RequestParam(required = false) Integer coachYearsMax) {
        // 如果有用户名/姓名模糊搜索，先查 user 表获取匹配的教练 user_id
        List<Integer> filterUserIds = null;
        if ((username != null && !username.isEmpty()) || (realName != null && !realName.isEmpty())) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<User>()
                    .eq(User::getRole, 2)
                    .like(username != null && !username.isEmpty(), User::getUsername, username)
                    .like(realName != null && !realName.isEmpty(), User::getRealName, realName);
            filterUserIds = userMapper.selectList(userWrapper).stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList());
            if (filterUserIds.isEmpty()) {
                return JsonResult.ok(new Page<>(page, size));
            }
        }

        // 查询教练表（如有筛选条件则按 user_id 过滤）
        LambdaQueryWrapper<Coach> coachWrapper = new LambdaQueryWrapper<Coach>()
                .orderByDesc(Coach::getCreateTime);
        if (filterUserIds != null) {
            coachWrapper.in(Coach::getUserId, filterUserIds);
        }
        if (vehicleType != null && !vehicleType.isEmpty()) {
            coachWrapper.apply("FIND_IN_SET({0}, vehicle_type)", vehicleType);
        }
        if (ratingMin != null) {
            coachWrapper.ge(Coach::getRating, ratingMin);
        }
        if (ratingMax != null) {
            coachWrapper.le(Coach::getRating, ratingMax);
        }
        if (coachYearsMin != null) {
            coachWrapper.ge(Coach::getCoachYears, coachYearsMin);
        }
        if (coachYearsMax != null) {
            coachWrapper.le(Coach::getCoachYears, coachYearsMax);
        }
        Page<Coach> coachPage = coachService.page(new Page<>(page, size), coachWrapper);

        // 批量查询关联的用户信息
        List<Integer> userIds = coachPage.getRecords().stream()
                .map(Coach::getUserId)
                .collect(Collectors.toList());
        Map<Integer, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getUserId, userIds)
            ).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));
        }

        // 合并数据
        List<Map<String, Object>> records = new ArrayList<>();
        for (Coach coach : coachPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            // 教练表字段
            item.put("coachId", coach.getCoachId());
            item.put("userId", coach.getUserId());
            item.put("rating", coach.getRating());
            item.put("coachYears", coach.getCoachYears());
            item.put("vehicleType", coach.getVehicleType());
            item.put("availableTime", coach.getAvailableTime());
            // 用户表字段
            User user = userMap.get(coach.getUserId());
            if (user != null) {
                item.put("realName", user.getRealName());
                item.put("username", user.getUsername());
                item.put("phone", user.getPhone());
            }
            records.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(coachPage.getCurrent(), coachPage.getSize(), coachPage.getTotal());
        result.setRecords(records);
        return JsonResult.ok(result);
    }

    @Operation(summary = "根据ID查询教练",
            description = "返回教练详情及关联的用户信息")
    @GetMapping("/{id}")
    public JsonResult<Map<String, Object>> getById(@PathVariable Integer id) {
        Coach coach = coachService.getById(id);
        if (coach == null) {
            return JsonResult.ok(null);
        }

        Map<String, Object> item = new LinkedHashMap<>();
        // 教练表字段
        item.put("coachId", coach.getCoachId());
        item.put("userId", coach.getUserId());
        item.put("rating", coach.getRating());
        item.put("coachYears", coach.getCoachYears());
        item.put("vehicleType", coach.getVehicleType());
        item.put("availableTime", coach.getAvailableTime());
        // 用户表字段
        User user = userMapper.selectById(coach.getUserId());
        if (user != null) {
            item.put("realName", user.getRealName());
            item.put("username", user.getUsername());
            item.put("phone", user.getPhone());
            item.put("idCard", user.getIdCard());
            item.put("avatar", user.getAvatar());
        }
        return JsonResult.ok(item);
    }

    @RequireRole(3)
    @Operation(summary = "新增教练")
    @PostMapping
    public JsonResult<Void> add(@RequestBody Coach coach) {
        coachService.save(coach);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改教练")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody Coach coach) {
        coach.setCoachId(id);
        coachService.updateById(coach);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除教练")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        coachService.removeById(id);
        return JsonResult.ok();
    }
}
