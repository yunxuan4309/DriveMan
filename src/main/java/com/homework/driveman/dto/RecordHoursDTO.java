package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "录入学时请求参数")
public class RecordHoursDTO {

    @NotNull(message = "约课ID不能为空")
    @Schema(description = "约课ID", example = "1")
    private Integer appointmentId;

    @NotNull(message = "学时时长不能为空")
    @DecimalMin(value = "0.5", message = "学时时长至少0.5小时")
    @Schema(description = "学时时长（小时）", example = "2.0")
    private BigDecimal duration;

    @NotNull(message = "科目类型不能为空")
    @Schema(description = "科目类型：1-科目一,2-科目二,3-科目三,4-科目四", example = "2")
    private Integer subjectType;

    @Schema(description = "备注", example = "科目二练习")
    private String remark;
}