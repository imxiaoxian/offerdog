package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.ArrayTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

/**
 * 题目收藏实体类
 *
 * @Author Zane
 * @CreateTime 2025/11/22 星期六 10:58
 */
@Data
@TableName(value = "question_favorites", autoResultMap = true)
public class FavoriteDO {

    /**
     * ID，主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "question_ids", jdbcType = JdbcType.ARRAY, typeHandler = ArrayTypeHandler.class)
    private Long[] questionIds;

    @TableField(value = "create_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createAt;
}
