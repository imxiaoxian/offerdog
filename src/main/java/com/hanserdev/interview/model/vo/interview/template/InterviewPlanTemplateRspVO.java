package com.hanserdev.interview.model.vo.interview.template;

import com.hanserdev.interview.model.dto.template.PlanStructureDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InterviewPlanTemplateRspVO {

    /**
     * 计划模板ID
     */
    private UUID planTemplateId;

    /**
     * 关联的面试模板ID
     */
    private UUID templateId;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 面试计划结构
     */
    private PlanStructureDTO planStructure;

    /**
     * 使用次数统计
     */
    private Integer usageCount;

    /**
     * 平均完成率
     */
    private BigDecimal avgCompletionRate;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
