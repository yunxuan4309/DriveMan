package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "设置空闲时间请求参数")
public class AvailableTimeDTO {

    @NotBlank(message = "空闲时间不能为空")
    @Schema(description = "空闲时间（JSON格式）", example = "{\"monday\":[\"09:00-12:00\",\"14:00-17:00\"],\"wednesday\":[\"09:00-12:00\"]}")
    private String availableTime;
}