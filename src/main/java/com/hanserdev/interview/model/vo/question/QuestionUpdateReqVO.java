package com.hanserdev.interview.model.vo.question;

import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionUpdateReqVO {

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
     * 答案
     */
    private String answer;

    /**
     * 提示信息
     */
    private String tips;

    /**
     * 难度等级
     */
    private QuestionDifficultyEnum difficulty;

    /**
     * 标签列表，每个标签最大长度为30个字符
     */
    private List<@Size(max = 30) String> tags;

    /**
     * 备注信息
     */
    private String remark;
}
