package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "批量设置可预约时间段")
public class UpdateTimeSlotsDTO {

    @NotNull(message = "时间段列表不能为空")
    @Schema(description = "时间段列表")
    private List<TimeSlotDTO> timeSlots;
}