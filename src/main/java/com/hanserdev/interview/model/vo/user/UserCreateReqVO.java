package com.hanserdev.interview.model.vo.user;

import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateReqVO {

    /**
     * 用户名，长度必须在3-50个字符之间
     */
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    /**
     * 密码，长度必须在8-100个字符之间
     */
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    /**
     * 用户角色，默认为普通用户
     */
    @NotNull
    private UserRoleEnum role = UserRoleEnum.USER;

    /**
     * 用户资料信息
     */
    @NotNull
    @Valid
    private UserProfileDTO profile;

    /**
     * 用户简历信息（可选）
     */
    @Valid
    private UserResumeDTO resume;

    /**
     * 账户是否激活，默认为true
     */
    private Boolean isActive = Boolean.TRUE;
}
