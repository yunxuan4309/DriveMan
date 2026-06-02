package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Notice;
import org.springframework.stereotype.Repository;

/**
 * 系统公告表 Mapper
 * 提供系统公告的增删改查基础操作
 */
@Repository
public interface NoticeMapper extends BaseMapper<Notice> {
}