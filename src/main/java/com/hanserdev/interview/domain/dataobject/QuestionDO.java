package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import com.hanserdev.interview.domain.typehandler.JsonbTypeHandler;
import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import com.hanserdev.interview.enums.QuestionSourceEnum;
import com.hanserdev.interview.model.dto.question.QuestionStatsDTO;
import lombok.Data;
import org.apache.ibatis.type.ArrayTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

@Data
@TableName(value = "questions", autoResultMap = true)
public class QuestionDO {

    /**
     * 题目ID，主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 题库ID
     */
    @TableField("bank_id")
    private Long bankId;

    /**
     * 岗位ID或行业ID，二选一
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 题目内容
     */
    @TableField("content")
    private String content;

    /**
     * 题目答案
     */
    @TableField("answer")
    private String answer;

    /**
     * 题目提示
     */
    @TableField("tips")
    private String tips;

    /**
     * 题目难度，使用自定义类型处理器处理枚举类型，easy medium hard，默认为medium
     */
    @TableField(value = "difficulty", jdbcType = JdbcType.OTHER)
    private QuestionDifficultyEnum difficulty;

    /**
     * 题目来源，使用自定义类型处理器处理枚举类型，根据创建者自动确认
     */
    @TableField(value = "source", jdbcType = JdbcType.OTHER)
    private QuestionSourceEnum source;

    /**
     * 题目标签数组，使用数组类型处理器处理
     */
    @TableField(value = "tags", jdbcType = JdbcType.ARRAY, typeHandler = ArrayTypeHandler.class)
    private String[] tags;

    /**
     * 题目统计信息，使用自定义类型处理器处理DTO对象
     */
    @TableField(value = "stats", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private QuestionStatsDTO stats;

    /**
     * 备注信息
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间，使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "created_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createdAt;

    /**
     * 更新时间，使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime updatedAt;

    /**
     * 删除时间，使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "deleted_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime deletedAt;

    /**
     * 向量化时间，使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "vector_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime vectorAt;
}
