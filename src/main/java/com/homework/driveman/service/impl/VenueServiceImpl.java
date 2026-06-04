package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Venue;
import com.homework.driveman.mapper.VenueMapper;
import com.homework.driveman.service.IVenueService;
import org.springframework.stereotype.Service;

/** 场地统一管理业务实现 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements IVenueService {
}
