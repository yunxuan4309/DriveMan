package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.File;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.IConfigService;
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
    private IConfigService configService;

    @Autowired
    private CoachMapper coachMapper;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 上传 ====================

    @Operation(summary = "上传文件",
            description = "fileType: id_card_front / id_card_back / physical_exam / registration_pdf / " +
                    "admission_ticket / training_record / coach_qualification / exam_score\n" +
                    "bizType: user_profile / enrollment / exam_ticket / registration_form / " +
                    "training_record / physical_exam / license_upgrade / coach_qualification / exam_score\n" +
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

    @Operation(summary = "分页查询当前登录用户自己的文件",
            description = "支持多维度过滤：bizType（业务分类）、fileType（文件格式）、keyword（文件名搜索）\n" +
                    "bizType 可选值: user_profile / enrollment / exam_ticket / registration_form / " +
                    "training_record / physical_exam / license_upgrade / coach_qualification / exam_score\n" +
                    "training_record / physical_exam / license_upgrade / coach_qualification")
    @GetMapping("/my")
    public JsonResult<Page<File>> listMyFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
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
        return JsonResult.ok(query.page(new Page<>(page, size)));
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

    @RequireRole(3)
    @Operation(summary = "按用户ID查询文件列表",
            description = "管理员查询指定用户的所有文件，用于录入体检结果等场景选择已有文件")
    @GetMapping("/by-user/{userId}")
    public JsonResult<List<File>> listByUser(@PathVariable Integer userId) {
        List<File> list = fileService.lambdaQuery()
                .eq(File::getUserId, userId)
                .orderByDesc(File::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    @Operation(summary = "管理员分页查询文件",
            description = "支持按真实姓名和角色过滤，返回分页文件列表附带用户姓名")
    @RequireRole(3)
    @GetMapping("/admin/query")
    public JsonResult<Page<Map<String, Object>>> adminQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @Parameter(description = "用户真实姓名（模糊搜索）") String realName,
            @RequestParam(required = false) @Parameter(description = "角色: 1-学员, 2-教练") Integer role,
            @RequestParam(required = false) @Parameter(description = "业务类型过滤") String bizType,
            @RequestParam(required = false) @Parameter(description = "文件分类过滤") String fileType) {
        return JsonResult.ok(fileService.pageAdminQuery(new Page<>(page, size), realName, role, bizType, fileType));
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
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            if (preview) {
                if (file.getMimeType() != null) {
                    contentType = MediaType.parseMediaType(file.getMimeType());
                } else {
                    // mime_type 为空时从扩展名推断
                    String fn = file.getFileName();
                    if (fn != null) {
                        String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
                        contentType = switch (ext) {
                            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                            case "png" -> MediaType.IMAGE_PNG;
                            case "gif" -> MediaType.IMAGE_GIF;
                            case "bmp" -> MediaType.valueOf("image/bmp");
                            case "webp" -> MediaType.valueOf("image/webp");
                            case "pdf" -> MediaType.APPLICATION_PDF;
                            default -> MediaType.APPLICATION_OCTET_STREAM;
                        };
                    }
                }
            }

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
        String schoolName = configService.getConfigValue("school_name");
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

    @Operation(summary = "预览文件（返回文件流，支持图片和PDF）",
            description = "根据文件类型自动设置Content-Type，图片和PDF可直接在浏览器中预览")
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Integer id) {
        File file = fileService.getById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Path.of(fileService.getAbsolutePath(file));
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 根据文件扩展名设置 Content-Type
            String fileName = file.getFileName();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (fileName != null) {
                String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                switch (ext) {
                    case "jpg", "jpeg" -> mediaType = MediaType.IMAGE_JPEG;
                    case "png" -> mediaType = MediaType.IMAGE_PNG;
                    case "gif" -> mediaType = MediaType.IMAGE_GIF;
                    case "bmp" -> mediaType = MediaType.valueOf("image/bmp");
                    case "webp" -> mediaType = MediaType.valueOf("image/webp");
                    case "pdf" -> mediaType = MediaType.APPLICATION_PDF;
                }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @RequireRole(3)
    @Operation(summary = "删除文件记录（逻辑删除，磁盘文件保留）")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        fileService.removeById(id);
        return JsonResult.ok();
    }
}
