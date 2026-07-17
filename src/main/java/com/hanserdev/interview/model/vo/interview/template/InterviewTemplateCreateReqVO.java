package com.hanserdev.interview.model.vo.interview.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.hanserdev.interview.model.dto.template.PlanStructureDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InterviewTemplateCreateReqVO {

    /**
     * 模板名称
     */
    @NotBlank
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 分类（前端工程师、后端工程师、HR面试等）
     */
    @NotBlank
    private String category;

    /**
     * 难度级别（junior/mid/senior）
     */
    @NotBlank
    @Pattern(regexp = "junior|mid|senior")
    private String difficultyLevel;

    /**
     * 预计时长（分钟）
     */
    @Min(1)
    private Integer estimatedDurationMinutes;

    /**
     * AI面试官的系统提示词模板
     */
    @NotBlank
    private String systemPrompt;

    /**
     * 默认的面试配置
     */
    private JsonNode defaultConfig;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 计划名称
     */
    @NotBlank
    private String planName;

    /**
     * 预定义的面试计划结构
     */
    @NotNull
    @Valid
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
     * 计划是否激活
     */
    private Boolean planIsActive;
}
