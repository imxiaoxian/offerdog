package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hanserdev.interview.domain.typehandler.JsonbTypeHandler;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import com.hanserdev.interview.enums.ConversationRoleEnum;
import com.hanserdev.interview.model.dto.conversation.ConversationQuickEvalDTO;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName(value = "conversation_messages", autoResultMap = true)
public class ConversationMessageDO {

    /**
     * 消息 ID，数据库默认生成 UUID
     */
    @TableId(value = "message_id")
    private UUID messageId;

    /**
     * 会话 ID，关联 interview_sessions 表
     */
    @TableField("session_id")
    private UUID sessionId;

    /**
     * 消息角色（面试官/候选人）
     */
    @TableField(value = "role", jdbcType = JdbcType.VARCHAR)
    private ConversationRoleEnum role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 关联的题目 ID，可为空
     */
    @TableField("related_question_id")
    private String relatedQuestionId;

    /**
     * 会话内的顺序号
     */
    @TableField("sequence_number")
    private Integer sequenceNumber;

    /**
     * token 统计
     */
    @TableField("token_count")
    private Integer tokenCount;

    @TableField("estimated_tokens")
    private Integer estimatedTokens;

    /**
     * AI 实时评估信息
     */
    @TableField(value = "ai_quick_eval", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private ConversationQuickEvalDTO aiQuickEval;

    /**
     * 消息时间
     */
    @TableField(value = "timestamp", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime timestamp;
}
