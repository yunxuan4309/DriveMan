package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "约课操作请求参数")
public class AppointmentActionDTO {

    @NotNull(message = "约课ID不能为空")
    @Schema(description = "约课ID", example = "1")
    private Integer appointmentId;

    @Schema(description = "拒绝原因（仅拒绝时需要）", example = "该时间段已满")
    private String rejectReason;
}