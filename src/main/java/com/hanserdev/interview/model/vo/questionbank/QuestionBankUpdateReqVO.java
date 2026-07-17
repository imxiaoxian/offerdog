package com.hanserdev.interview.model.vo.questionbank;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionBankUpdateReqVO {

    /**
     * 题库名称，最大长度100个字符
     */
    @Size(max = 100)
    private String name;

    /**
     * 题库描述，最大长度1000个字符
     */
    @Size(max = 1000)
    private String description;

    /**
     * 岗位ID
     */
    private Long categoryId;
}
