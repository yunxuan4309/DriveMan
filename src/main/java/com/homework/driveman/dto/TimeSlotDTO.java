package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "常规空闲时间段（仅供学员参考）")
public class TimeSlotDTO {

    @NotBlank(message = "星期几不能为空")
    @Pattern(regexp = "monday|tuesday|wednesday|thursday|friday|saturday|sunday",
            message = "星期几必须是英文全小写")
    @Schema(description = "星期几（英文小写）", example = "monday")
    private String dayOfWeek;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "开始时间格式 HH:mm")
    @Schema(description = "开始时间", example = "09:00")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "结束时间格式 HH:mm")
    @Schema(description = "结束时间", example = "12:00")
    private String endTime;
}