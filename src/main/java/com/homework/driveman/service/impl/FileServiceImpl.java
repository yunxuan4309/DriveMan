package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.File;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.FileMapper;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 文件业务实现 — 本地磁盘存储
 * 存储结构: {uploadPath}/{fileType}/{userId}_{timestamp}_{originalName}
 */
@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements IFileService {

    /** 允许的文件分类 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "id_card_front", "id_card_back", "physical_exam",
            "registration_pdf", "admission_ticket"
    );

    /** 各类文件允许的后缀（小写，不含点） */
    private static final java.util.Map<String, Set<String>> ALLOWED_EXTENSIONS =
            java.util.Map.of(
                    "id_card_front", Set.of("jpg", "jpeg", "png", "bmp", "webp"),
                    "id_card_back", Set.of("jpg", "jpeg", "png", "bmp", "webp"),
                    "physical_exam", Set.of("jpg", "jpeg", "png", "bmp", "webp", "pdf"),
                    "registration_pdf", Set.of("pdf"),
                    "admission_ticket", Set.of("pdf")
            );

    @Value("${drive.upload.path:./upload-files}")
    private String uploadPath;

    @Override
    public File upload(Integer userId, MultipartFile multipartFile, String fileType) {
        // 校验文件分类
        if (!ALLOWED_TYPES.contains(fileType)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "不支持的文件分类: " + fileType);
        }

        // 校验文件是否为空
        if (multipartFile.isEmpty()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "上传文件为空");
        }

        // 校验文件扩展名
        String originalName = multipartFile.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            Set<String> allowedExts = ALLOWED_EXTENSIONS.get(fileType);
            if (allowedExts != null && !allowedExts.contains(ext)) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "文件类型 " + fileType + " 不支持 ." + ext + " 格式，仅支持: " + String.join(", ", allowedExts));
            }
        }

        // 构造存储目录: ./upload-files/{fileType}/
        String typeDir = fileType + "/";
        Path targetDir = Paths.get(uploadPath, typeDir);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new ServiceException(ServiceCode.ERROR_INSERT, "创建目录失败");
        }

        // 生成文件名: {userId}_{时间戳}_{原始文件名}
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String storedName = userId + "_" + timestamp + "_" + (originalName != null ? originalName : "file");

        // 保存文件到磁盘
        Path targetFile = targetDir.resolve(storedName);
        try {
            multipartFile.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("文件写入失败: {}", targetFile, e);
            throw new ServiceException(ServiceCode.ERROR_INSERT, "文件保存失败");
        }

        // 持久化文件记录到数据库
        File file = new File();
        file.setUserId(userId);
        file.setFileName(originalName);
        file.setFilePath(typeDir + storedName);
        file.setFileType(fileType);
        file.setUploadTime(LocalDateTime.now());
        save(file);

        log.info("文件上传成功: id={}, type={}, name={}", file.getId(), fileType, originalName);
        return file;
    }

    @Override
    public File saveRecord(Integer userId, String filePath, String fileName, String fileType) {
        File file = new File();
        file.setUserId(userId);
        file.setFileName(fileName);
        file.setFilePath(filePath);
        file.setFileType(fileType);
        file.setUploadTime(LocalDateTime.now());
        save(file);
        log.info("文件记录保存成功: id={}, type={}, path={}", file.getId(), fileType, filePath);
        return file;
    }

    @Override
    public String getAbsolutePath(File file) {
        return Paths.get(uploadPath, file.getFilePath()).toString();
    }

    @Override
    public String getUrlPath(File file) {
        return "/uploads/" + file.getFilePath().replace("\\", "/");
    }
}
