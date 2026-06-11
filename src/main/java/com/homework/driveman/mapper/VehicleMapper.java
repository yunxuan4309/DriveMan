package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 车辆表 Mapper
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    /**
     * 将所有当前有空闲排班的空闲车辆设为"使用中"
     * 条件：车辆状态为 1(空闲) && 存在已通过排班且当前时间在排班时间范围内
     */
    @Update("UPDATE vehicle v " +
            "SET v.status = 2 " +
            "WHERE v.status = 1 AND v.is_deleted = 0 " +
            "AND EXISTS (SELECT 1 FROM coach_schedule cs " +
            "            WHERE cs.vehicle_id = v.id " +
            "              AND cs.status = 1 " +
            "              AND cs.start_time <= NOW() " +
            "              AND cs.end_time >= NOW())")
    int updateToInUse();

    /**
     * 将所有当前无有效排班的使用中车辆设回"空闲"
     * 条件：车辆状态为 2(使用中) && 不存在已通过且当前时间在范围内的排班
     */
    @Update("UPDATE vehicle v " +
            "SET v.status = 1 " +
            "WHERE v.status = 2 AND v.is_deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM coach_schedule cs " +
            "                WHERE cs.vehicle_id = v.id " +
            "                  AND cs.status = 1 " +
            "                  AND cs.start_time <= NOW() " +
            "                  AND cs.end_time >= NOW())")
    int updateToIdle();
}
