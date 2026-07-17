package com.hanserdev.interview.model.vo.questionbank;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBankQueryReqVO {

    /**
     * 页码，最小值为1
     */
    @Min(1)
    private Long pageNo = 1L;

    /**
     * 每页数量，最小值为1，最大值为100
     */
    @Min(1)
    @Max(100)
    private Long pageSize = 10L;

    /**
     * 题库名称
     */
    private String name;

    /**
     * 岗位ID
     */
    private Long categoryId;

    /**
     * 多个岗位 ID（OR）；与 {@link #categoryId} 同时存在时优先使用本字段
     */
    @Size(max = 128)
    private List<Long> categoryIds;
}
