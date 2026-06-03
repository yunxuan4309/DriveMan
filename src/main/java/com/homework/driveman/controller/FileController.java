package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.File;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.ConfigMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.service.IPdfService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件管理控制器 — 上传/下载/预览/查询/删除
 * 三端统一入口，通过 @RequireRole + hasAccess 控制权限
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private IFileService fileService;

    @Autowired
    private IPdfService pdfService;

    @Autowired
    private IUserService userService;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private ConfigMapper configMapper;

    @Autowired
    private CoachMapper coachMapper;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 上传 ====================

    @Operation(summary = "上传文件",
            description = "fileType: id_card_front / id_card_back / physical_exam / registration_pdf / " +
                    "admission_ticket / training_record / coach_qualification\n" +
                    "bizType: user_profile / enrollment / exam_ticket / registration_form / " +
                    "training_record / physical_exam / license_upgrade / coach_qualification\n" +
                    "普通用户只能上传自己的文件，管理员可上传任意用户文件")
    @PostMapping("/upload")
    public JsonResult<File> upload(@RequestParam Integer userId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam String fileType,
                                   @RequestParam(required = false) String bizType,
                                   @RequestParam(required = false) Integer bizId,
                                   HttpServletRequest request) {
        CurrentUser currentUser = getCurrentUser(request);
        // 非管理员只能上传自己的文件
        if (currentUser.getRole() != 3 && !currentUser.getUserId().equals(userId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权替他人上传文件");
        }
        File saved = fileService.upload(userId, file, fileType, bizType, bizId);
        return JsonResult.ok(saved);
    }

    // ==================== 查询 ====================

    @Operation(summary = "查询当前登录用户自己的文件",
            description = "支持多维度过滤：bizType（业务分类）、fileType（文件格式）、keyword（文件名搜索）\n" +
                    "bizType 可选值: user_profile / enrollment / exam_ticket / registration_form / " +
                    "training_record / physical_exam / license_upgrade / coach_qualification")
    @GetMapping("/my")
    public JsonResult<List<File>> listMyFiles(
            @RequestParam(required = false) @Parameter(description = "业务类型过滤") String bizType,
            @RequestParam(required = false) @Parameter(description = "文件分类过滤（旧字段）") String fileType,
            @RequestParam(required = false) @Parameter(description = "文件名关键词搜索") String keyword,
            HttpServletRequest request) {
        CurrentUser currentUser = getCurrentUser(request);
        var query = fileService.lambdaQuery()
                .eq(File::getUserId, currentUser.getUserId())
                .orderByDesc(File::getCreateTime);
        if (bizType != null) {
            query.eq(File::getBizType, bizType);
        }
        if (fileType != null) {
            query.eq(File::getFileType, fileType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(File::getFileName, keyword);
        }
        return JsonResult.ok(query.list());
    }

    @Operation(summary = "按业务查询附件")
    @GetMapping("/biz/{bizType}/{bizId}")
    public JsonResult<List<File>> listByBiz(@PathVariable String bizType,
                                            @PathVariable Integer bizId,
                                            HttpServletRequest request) {
        CurrentUser currentUser = getCurrentUser(request);
        List<File> list = fileService.lambdaQuery()
                .eq(File::getBizType, bizType)
                .eq(File::getBizId, bizId)
                .orderByDesc(File::getCreateTime)
                .list();

        // 非管理员只能查自己相关的业务
        if (currentUser.getRole() != 3) {
            list.removeIf(f -> !f.getUserId().equals(currentUser.getUserId()));
        }
        return JsonResult.ok(list);
    }

    @Operation(summary = "根据ID查询文件详情")
    @GetMapping("/{id}")
    public JsonResult<File> getById(@PathVariable Integer id,
                                    HttpServletRequest request) {
        File file = fileService.getById(id);
        if (file == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "文件不存在");
        }
        CurrentUser currentUser = getCurrentUser(request);
        if (!fileService.hasAccess(file, currentUser.getUserId(), currentUser.getRole())) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权访问该文件");
        }
        return JsonResult.ok(file);
    }

    @Operation(summary = "管理员按条件查询文件",
            description = "支持按真实姓名和角色过滤（至少传一个条件），返回文件列表附带用户姓名")
    @RequireRole(3)
    @GetMapping("/admin/query")
    public JsonResult<List<Map<String, Object>>> adminQuery(
            @RequestParam(required = false) @Parameter(description = "用户真实姓名（模糊搜索）") String realName,
            @RequestParam(required = false) @Parameter(description = "角色: 1-学员, 2-教练") Integer role) {
        // 至少需要一个筛选条件
        if (realName == null && role == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "请至少输入姓名或选择角色进行查询");
        }

        // 1. 按条件查用户 ID 列表
        var userQuery = userService.lambdaQuery();
        if (realName != null && !realName.isEmpty()) {
            userQuery.like(User::getRealName, realName);
        }
        if (role != null) {
            userQuery.eq(User::getRole, role);
        }
        List<User> matchedUsers = userQuery.list();
        if (matchedUsers.isEmpty()) {
            return JsonResult.ok(List.of());
        }
        List<Integer> userIds = matchedUsers.stream().map(User::getUserId).toList();

        // 2. 查这些用户的文件
        List<File> files = fileService.lambdaQuery()
                .in(File::getUserId, userIds)
                .orderByDesc(File::getCreateTime)
                .list();

        // 3. 构建 userId -> realName 映射
        Map<Integer, String> nameMap = matchedUsers.stream()
                .collect(java.util.stream.Collectors.toMap(User::getUserId, User::getRealName));

        // 4. 组装返回（文件信息 + 用户姓名）
        List<Map<String, Object>> result = files.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("userId", f.getUserId());
            m.put("realName", nameMap.get(f.getUserId()));
            m.put("fileName", f.getFileName());
            m.put("filePath", f.getFilePath());
            m.put("fileSize", f.getFileSize());
            m.put("mimeType", f.getMimeType());
            m.put("fileType", f.getFileType());
            m.put("bizType", f.getBizType());
            m.put("bizId", f.getBizId());
            m.put("uploadTime", f.getUploadTime());
            m.put("createTime", f.getCreateTime());
            return m;
        }).toList();

        return JsonResult.ok(result);
    }

    // ==================== 下载 / 预览 ====================

    @Operation(summary = "下载或预览文件",
            description = "preview=false（默认）→ 附件下载；preview=true → 浏览器内预览（PDF/图片）")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Integer id,
                                             @RequestParam(defaultValue = "false") boolean preview,
                                             HttpServletRequest request) {
        File file = fileService.getById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        CurrentUser currentUser = getCurrentUser(request);
        if (!fileService.hasAccess(file, currentUser.getUserId(), currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Path filePath = Path.of(fileService.getAbsolutePath(file));
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String encodedFileName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");

            // 确定 Content-Type: 预览用实际类型，下载用 octet-stream
            MediaType contentType = preview && file.getMimeType() != null
                    ? MediaType.parseMediaType(file.getMimeType())
                    : MediaType.APPLICATION_OCTET_STREAM;

            ContentDisposition disposition = preview
                    ? ContentDisposition.inline().filename(encodedFileName, StandardCharsets.UTF_8).build()
                    : ContentDisposition.attachment().filename(encodedFileName, StandardCharsets.UTF_8).build();

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== 培训记录表生成 ====================

    @Operation(summary = "生成培训记录表 PDF",
            description = "学员调用生成自己的培训记录表（需绑定教练且有学时数据）")
    @PostMapping("/generate-training-record")
    public JsonResult<File> generateTrainingRecord(@RequestParam Integer studentId,
                                                   HttpServletRequest request) {
        CurrentUser currentUser = getCurrentUser(request);
        // 非管理员只能为自己生成
        if (currentUser.getRole() != 3 && !currentUser.getUserId().equals(studentId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权替他人生成培训记录");
        }

        // 1. 查学员信息
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在");
        }

        // 2. 查绑定的教练
        StudentCoach sc = studentCoachMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, studentId)
                        .eq(StudentCoach::getStatus, 1)
                        .last("LIMIT 1"));
        String coachName = null;
        if (sc != null) {
            Coach coach = coachMapper.selectById(sc.getCoachId());
            if (coach != null) {
                User coachUser = userService.getById(coach.getUserId());
                if (coachUser != null) {
                    coachName = coachUser.getRealName();
                }
            }
        }

        // 3. 查驾校名称（从 config 表读取）
        String schoolName = configMapper.getConfigValue("school_name");
        if (schoolName == null) schoolName = "";

        // 4. 查各科目累计学时
        String licenseType = student.getLicenseType();
        Map<Integer, BigDecimal> hoursPerSubject = new LinkedHashMap<>();
        if (licenseType != null) {
            for (int subject = 1; subject <= 4; subject++) {
                BigDecimal hours = trainingRecordMapper.sumTrainingHours(studentId, licenseType, subject);
                hoursPerSubject.put(subject, hours);
            }
        }

        // 5. 生成 PDF
        String relativePath = pdfService.generateTrainingRecord(student, coachName, schoolName, hoursPerSubject, licenseType);

        // 6. 保存文件记录
        String fileName = "培训记录表_" + student.getRealName() + ".pdf";
        File file = fileService.saveRecord(studentId, relativePath, fileName, "training_record",
                "training_record", studentId);

        return JsonResult.ok(file);
    }

    // ==================== 删除 ====================

    @RequireRole(3)
    @Operation(summary = "删除文件记录（逻辑删除，磁盘文件保留）")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        fileService.removeById(id);
        return JsonResult.ok();
    }
}
