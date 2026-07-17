package com.hanserdev.interview.model.vo.user;

import com.hanserdev.interview.enums.UserRoleEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserQueryReqVO {

    @Min(1)
    private Long pageNo = 1L;

    @Min(1)
    @Max(100)
    private Long pageSize = 10L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色：user - 普通用户，admin - 管理员，vip - VIP用户
     */
    private UserRoleEnum role;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 技能
     */
    private String skill;
}
