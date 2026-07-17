package com.hanserdev.interview.model.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserEmailLoginByPasswordReqVO {

    /**
     * 用户邮箱地址
     * 必填项，需符合邮箱格式，最大长度100个字符
     */
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    /**
     * 用户密码
     * 必填项，最小长度8个字符，最大长度100个字符
     */
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}
