package com.hanserdev.interview.model.vo.interview;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 单次已生成报告的能力快照，用于成长曲线可视化。
 */
@Data
@Builder
public class InterviewGrowthPointRspVO {

    private UUID sessionId;
    private LocalDateTime reportCreatedAt;
    private BigDecimal overallScore;
    private Double technicalKnowledge;
    private Double communication;
    private Double problemSolving;
    private Double systemDesign;
    private Double practicalExperience;
    private Double learningAbility;
}
