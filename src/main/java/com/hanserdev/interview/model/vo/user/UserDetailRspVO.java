package com.hanserdev.interview.model.vo.user;

import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDetailRspVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private UserRoleEnum role;

    /**
     * 用户资料信息
     */
    private UserProfileDTO profile;

    /**
     * 用户简历信息
     */
    private UserResumeDTO resume;

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
