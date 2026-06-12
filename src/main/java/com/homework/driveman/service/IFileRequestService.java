package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.FileRequest;

import java.util.Map;

public interface IFileRequestService extends IService<FileRequest> {

    /** 创建文件提交请求 */
    FileRequest create(Integer requesterId, Integer targetUserId, String title,
                       String description, String bizType, Integer bizId,
                       String fileType, String remark, String deadline);

    /** 目标用户完成上传后，自动标记请求为已完成 */
    void markCompleted(Integer requestId);

    /** 取消请求 */
    void cancel(Integer requestId);

    /** 查询目标用户的待提交请求 */
    Page<Map<String, Object>> pageByTargetUser(Page<FileRequest> page, Integer targetUserId);

    /** 管理员分页查询所有请求 */
    Page<Map<String, Object>> pageAll(Page<FileRequest> page, String targetName, Integer status);

    /** 查询目标用户的未完成请求数（用于登录提醒） */
    int countPending(Integer targetUserId);
}
