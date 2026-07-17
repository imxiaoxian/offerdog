package com.hanserdev.interview.model.dto.question;

import lombok.Data;

@Data
public class QuestionStatsDTO {

    /**
     * 浏览次数
     */
    private long views;
    
    /**
     * 点赞数
     */
    private long likes;
    
    /**
     * 收藏数
     */
    private long favorites;
}
