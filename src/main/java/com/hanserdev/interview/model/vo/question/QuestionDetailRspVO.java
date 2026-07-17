package com.hanserdev.interview.model.vo.question;

import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import com.hanserdev.interview.enums.QuestionSourceEnum;
import com.hanserdev.interview.model.dto.question.QuestionStatsDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionDetailRspVO {

    /**
     * 题目ID
     */
    private Long id;

    /**
     * 题库ID
     */
    private Long bankId;

    /**
     * 岗位ID/行业ID
     */
    private Long categoryId;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 题目答案
     */
    private String answer;

    /**
     * 题目提示
     */
    private String tips;

    /**
     * 题目难度，枚举：EASY-简单，MEDIUM-中等，HARD-困难
     */
    private QuestionDifficultyEnum difficulty;

    /**
     * 题目来源
     */
    private QuestionSourceEnum source;

    /**
     * 题目标签列表
     */
    private List<String> tags;

    /**
     * 题目统计数据
     */
    private QuestionStatsDTO stats;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
