package com.hanserdev.interview.model.vo.questionbank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionBankCreateReqVO {

    /**
     * 题库名称
     */
    @NotBlank
    @Size(max = 100)
    private String name;

    /**
     * 题库描述
     */
    @Size(max = 1000)
    private String description;

    /**
     * 岗位ID
     */
    @NotNull
    private Long categoryId;

}
