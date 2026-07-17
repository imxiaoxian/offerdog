package com.hanserdev.interview.model.vo.user;

import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateReqVO {

    /**
     * 用户名，长度限制为3-50个字符
     */
    @Size(min = 3, max = 50)
    private String username;

    /**
     * 密码，长度限制为8-100个字符
     */
    @Size(min = 8, max = 100)
    private String password;

    /**
     * 用户角色
     */
    private UserRoleEnum role;

    /**
     * 用户资料信息，不能为空且需要验证
     */
    @NotNull
    @Valid
    private UserProfileDTO profile;

    /**
     * 用户简历信息，可为空但若存在则需要验证
     */
    @Valid
    private UserResumeDTO resume;

    /**
     * 账户是否激活状态
     */
    private Boolean isActive;
}
