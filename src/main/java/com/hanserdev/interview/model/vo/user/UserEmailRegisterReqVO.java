package com.hanserdev.interview.model.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserEmailRegisterReqVO {

    /**
     * 用户名
     */
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    /**
     * 邮箱地址
     */
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    /**
     * 密码
     */
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    /**
     * 验证码
     */
    @NotBlank
    @Size(min = 4, max = 10)
    private String verificationCode;
}
