package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

@Data
@TableName(value = "interview_experience", autoResultMap = true)
public class InterviewExperienceDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("company_name")
    private String companyName;

    @TableField("position")
    private String position;

    @TableField("experience_type")
    private String experienceType;

    @TableField("content")
    private String content;

    @TableField("formatted_content")
    private String formattedContent;

    @TableField("source_url")
    private String sourceUrl;

    @TableField("source")
    private String source;

    @TableField("author")
    private String author;

    @TableField("views")
    private Integer views;

    @TableField("likes")
    private Integer likes;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "created_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime updatedAt;

    @TableField(value = "deleted_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime deletedAt;
}
