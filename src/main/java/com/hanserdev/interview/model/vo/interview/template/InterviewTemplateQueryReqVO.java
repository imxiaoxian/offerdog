package com.hanserdev.interview.model.vo.interview.template;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class InterviewTemplateQueryReqVO {

    /**
     * 页码
     */
    @Min(1)
    private Long pageNo = 1L;

    /**
     * 每页数量
     */
    @Min(1)
    @Max(100)
    private Long pageSize = 10L;

    /**
     * 模板名称（模糊匹配）
     */
    private String templateName;

    /**
     * 分类
     */
    private String category;

    /**
     * 难度级别
     */
    private String difficultyLevel;

    /**
     * 是否激活
     */
    private Boolean isActive;
}
