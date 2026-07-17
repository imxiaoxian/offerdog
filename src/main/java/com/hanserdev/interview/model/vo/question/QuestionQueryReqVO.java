package com.hanserdev.interview.model.vo.question;

import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionQueryReqVO {

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
     * 题目关键词搜索
     */
    private String keyword;

    /**
     * 答案关键词搜索
     */
    private String answerKeyword;

    /**
     * 题目难度，枚举值EASY、MEDIUM、HARD
     */
    private QuestionDifficultyEnum difficulty;

    /**
     * 标签列表筛选
     */
    private List<String> tags;

    /**
     * 题库ID筛选
     */
    private Long bankId;

    /**
     * 行业/岗位ID筛选
     */
    private Long categoryId;

    /**
     * 多个岗位 ID（OR）；与 {@link #categoryId} 同时存在时优先使用本字段
     */
    @Size(max = 128)
    private List<Long> categoryIds;
}
