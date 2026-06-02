package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Notice;
import com.homework.driveman.mapper.NoticeMapper;
import com.homework.driveman.service.INoticeService;
import org.springframework.stereotype.Service;

/**
 * 系统公告业务实现
 * 继承 MyBatis-Plus ServiceImpl，提供基础 CRUD
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements INoticeService {
}