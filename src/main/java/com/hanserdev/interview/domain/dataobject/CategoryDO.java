package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hanserdev.interview.domain.typehandler.JsonbTypeHandler;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.ArrayTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "categories", autoResultMap = true)
public class CategoryDO {

    /**
     * 分类唯一标识符
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    @TableField("name")
    private String name;

    /**
     * 父级分类ID，用于构建分类层级结构
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类层级，表示该分类在树形结构中的深度
     */
    @TableField("level")
    private Integer level;

    /**
     * 分类路径，记录从根节点到当前节点的完整路径
     * 使用数组形式存储各级分类ID
     */
    @TableField(value = "path", jdbcType = JdbcType.ARRAY, typeHandler = ArrayTypeHandler.class)
    private Integer[] path;

    /**
     * 分类元数据，存储额外的分类属性信息
     * 使用JSONB格式存储键值对数据
     */
    @TableField(value = "metadata", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 记录创建时间
     * 使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "created_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     * 使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime updatedAt;

    /**
     * 记录删除时间，用于软删除功能
     * 使用PostgreSQL的带时区时间戳类型
     */
    @TableField(value = "deleted_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime deletedAt;
}
