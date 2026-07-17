package com.hanserdev.interview.model.vo.user;

import com.hanserdev.interview.enums.UserRoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginRspVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 用户角色
     */
    private UserRoleEnum role;

    /**
     * 账户是否激活
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
