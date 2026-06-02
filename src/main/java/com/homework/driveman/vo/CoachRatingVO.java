package com.homework.driveman.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "教练评分")
public class CoachRatingVO {

    @Schema(description = "教练ID")
    private Integer coachId;

    @Schema(description = "评分（1.0-5.0）")
    private BigDecimal rating;
}