package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.vo.StudentListVO;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学员管理控制器 — 仅管理学员角色（role=1）的信息
 */
@Tag(name = "学员管理")
@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private IUserService userService;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

    @RequireRole(3)
    @Operation(summary = "分页查询学员",
            description = "支持多条件组合筛选（手机号/身份证/驾照类型模糊查询，报名状态精确匹配，结业状态过滤）")
    @GetMapping
    public JsonResult<Page<StudentListVO>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String username,
                                                @RequestParam(required = false) String realName,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) String idCard,
                                                @RequestParam(required = false) String licenseType,
                                                @RequestParam(required = false) Boolean allPassed,
                                                @RequestParam(required = false) String address) {
        // 1. 基础筛选条件（不含 allPassed，因为它是计算字段）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .eq(status != null, User::getStatus, status)
                .like(username != null && !username.isEmpty(), User::getUsername, username)
                .like(realName != null && !realName.isEmpty(), User::getRealName, realName)
                .like(phone != null && !phone.isEmpty(), User::getPhone, phone)
                .like(idCard != null && !idCard.isEmpty(), User::getIdCard, idCard)
                .like(licenseType != null && !licenseType.isEmpty(), User::getLicenseType, licenseType)
                .like(address != null && !address.isEmpty(), User::getAddress, address)
                .orderByDesc(User::getCreateTime);

        // 2. 获取学员数据
        List<User> users;
        long total;
        if (allPassed != null) {
            // 需要按结业筛选 → 查全量，计算后再手工分页
            users = userService.list(wrapper);
            total = users.size();
        } else {
            // 常规分页查询
            Page<User> pageData = userService.page(new Page<>(page, size), wrapper);
            users = pageData.getRecords();
            total = pageData.getTotal();
        }

        // 3. 构造结果页
        Page<StudentListVO> result = new Page<>(page, size, total);
        if (users.isEmpty()) {
            result.setRecords(Collections.emptyList());
            return JsonResult.ok(result);
        }

        // 4. 批量查询已通过科目
        List<Integer> studentIds = users.stream().map(User::getUserId).collect(Collectors.toList());
        List<ExamRegistration> passedExams = examRegistrationMapper.selectList(
                new LambdaQueryWrapper<ExamRegistration>()
                        .in(ExamRegistration::getStudentId, studentIds)
                        .eq(ExamRegistration::getPassStatus, 1));
        Map<Integer, Set<Integer>> studentPassedMap = new HashMap<>();
        for (ExamRegistration er : passedExams) {
            studentPassedMap.computeIfAbsent(er.getStudentId(), k -> new HashSet<>())
                    .add(er.getSubject());
        }

        // 5. 查询车型科目配置
        Set<String> licenseTypes = users.stream()
                .map(User::getLicenseType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, List<Integer>> requiredSubjectsMap = Collections.emptyMap();
        if (!licenseTypes.isEmpty()) {
            requiredSubjectsMap = licenseConfigMapper.selectList(
                            new LambdaQueryWrapper<LicenseConfig>()
                                    .in(LicenseConfig::getLicenseType, licenseTypes))
                    .stream()
                    .collect(Collectors.groupingBy(
                            LicenseConfig::getLicenseType,
                            Collectors.mapping(LicenseConfig::getSubject, Collectors.toList())));
        }

        // 6. 组装 StudentListVO
        List<StudentListVO> voList = new ArrayList<>(users.size());
        for (User u : users) {
            StudentListVO vo = new StudentListVO();
            BeanUtils.copyProperties(u, vo);

            List<Integer> required = requiredSubjectsMap.getOrDefault(u.getLicenseType(), Collections.emptyList());
            Set<Integer> passed = studentPassedMap.getOrDefault(u.getUserId(), Collections.emptySet());

            vo.setTotalSubjects(required.size());
            vo.setPassedCount((int) required.stream().filter(passed::contains).count());
            vo.setAllPassed(!required.isEmpty() && required.stream().allMatch(passed::contains));
            voList.add(vo);
        }

        // 7. 按结业状态筛选（如有）
        if (allPassed != null) {
            voList = voList.stream()
                    .filter(v -> Boolean.TRUE.equals(v.getAllPassed()) == allPassed)
                    .collect(Collectors.toList());
            long filteredTotal = voList.size();
            result = new Page<>(page, size, filteredTotal);

            int fromIndex = (page - 1) * size;
            if (fromIndex < voList.size()) {
                int toIndex = Math.min(fromIndex + size, voList.size());
                result.setRecords(voList.subList(fromIndex, toIndex));
            } else {
                result.setRecords(Collections.emptyList());
            }
            return JsonResult.ok(result);
        }

        result.setRecords(voList);
        return JsonResult.ok(result);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询学员")
    @GetMapping("/{id}")
    public JsonResult<User> getById(@PathVariable Integer id) {
        User user = userService.getById(id);
        if (user != null && user.getRole() != 1) {
            return JsonResult.ok(null);
        }
        return JsonResult.ok(user);
    }

    @RequireRole(3)
    @Operation(summary = "新增学员",
            description = "新增用户，角色自动设为学员，密码自动 BCrypt 加密")
    @PostMapping
    public JsonResult<Void> add(@RequestBody User user) {
        user.setRole(1);
        userService.register(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改学员信息",
            description = "密码传空字符串或 null 不会覆盖原密码")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody User user) {
        user.setUserId(id);
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(null);
        }
        userService.updateById(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除学员（逻辑删除）")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        userService.removeById(id);
        return JsonResult.ok();
    }
}
