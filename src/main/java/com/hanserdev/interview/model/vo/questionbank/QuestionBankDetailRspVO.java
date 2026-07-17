package com.hanserdev.interview.model.vo.questionbank;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionBankDetailRspVO {

    /**
     * 题库ID
     */
    private Long id;
    
    /**
     * 题库名称
     */
    private String name;
    
    /**
     * 题库描述
     */
    private String description;
    
    /**
     * 岗位ID
     */
    private Long categoryId;
    
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
