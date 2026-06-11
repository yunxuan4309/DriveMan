package com.homework.driveman.task;

import com.homework.driveman.mapper.VehicleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 车辆状态同步定时任务
 * <p>
 * 每分钟检查 coach_schedule 中已通过的排班，自动切换车辆状态：
 * <ul>
 *   <li>有排班正在进行（start_time ≤ now ≤ end_time）→ 空闲(1) → 使用中(2)</li>
 *   <li>无排班正在进行 → 使用中(2) → 空闲(1)</li>
 *   <li>不覆盖维修(3) / 报废(4) — 管理员手动优先级更高</li>
 * </ul>
 */
@Component
public class VehicleStatusSyncTask {

    private static final Logger log = LoggerFactory.getLogger(VehicleStatusSyncTask.class);

    @Autowired
    private VehicleMapper vehicleMapper;

    /**
     * 每分钟执行一次状态同步
     */
    @Scheduled(fixedRate = 60_000)
    public void syncVehicleStatus() {
        try {
            int toInUse = vehicleMapper.updateToInUse();
            int toIdle = vehicleMapper.updateToIdle();
            if (toInUse > 0 || toIdle > 0) {
                log.info("车辆状态同步完成：{} 辆 → 使用中, {} 辆 → 空闲", toInUse, toIdle);
            }
        } catch (Exception e) {
            log.error("车辆状态同步异常", e);
        }
    }
}
