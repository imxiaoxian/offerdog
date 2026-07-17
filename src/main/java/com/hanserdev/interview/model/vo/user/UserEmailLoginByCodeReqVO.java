package com.hanserdev.interview.model.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserEmailLoginByCodeReqVO {

    /**
     * 用户邮箱地址
     * 必填项，需符合邮箱格式，最大长度100个字符
     */
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    /**
     * 验证码
     * 必填项，长度在4到10个字符之间
     */
    @NotBlank
    @Size(min = 4, max = 10)
    private String verificationCode;
}
