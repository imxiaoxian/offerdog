package com.hanserdev.interview.model.vo.question;

import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import com.hanserdev.interview.enums.QuestionSourceEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionSummaryRspVO {

    /**
     * 题目ID
     */
    private Long id;
    
    /**
     * 题库ID
     */
    private Long bankId;
    
    /**
     * 行业/岗位ID
     */
    private Long categoryId;
    
    /**
     * 题目内容
     */
    private String content;
    
    /**
     * 题目难度
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
     * 创建时间
     */
    private LocalDateTime createdAt;
}
