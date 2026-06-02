package com.homework.driveman.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "教练工作量统计")
public class CoachWorkloadVO {

    @Schema(description = "当前绑定学员数")
    private Integer totalStudents;

    @Schema(description = "总培训学时")
    private BigDecimal totalHours;

    @Schema(description = "四科全通过率（0-100）", example = "75.5")
    private Double passRate;
}