package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hanserdev.interview.domain.typehandler.JsonbTypeHandler;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

@Data
@TableName(value = "users", autoResultMap = true)
public class UserDO {

    /**
     * 用户ID，主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码哈希值
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户角色枚举
     */
    @TableField(value = "role", jdbcType = JdbcType.OTHER)
    private UserRoleEnum role;

    /**
     * 用户个人资料信息，使用JSONB格式存储
     */
    @TableField(value = "profile", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private UserProfileDTO profile;

    /**
     * 用户简历信息，使用JSONB格式存储
     */
    @TableField(value = "resume", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private UserResumeDTO resume;

    /**
     * 账户是否激活状态
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 创建时间，使用PostgreSQL的timestamptz类型
     */
    @TableField(value = "created_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createdAt;

    /**
     * 更新时间，使用PostgreSQL的timestamptz类型
     */
    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime updatedAt;

    /**
     * 删除时间，使用PostgreSQL的timestamptz类型
     */
    @TableField(value = "deleted_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime deletedAt;
}
