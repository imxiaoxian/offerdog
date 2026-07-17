package com.hanserdev.interview.model.dto.interview;

import lombok.Builder;
import lombok.Data;

/**
 * 面试动作DTO
 * 表示AI判断的下一步动作
 */
@Data
@Builder
public class InterviewActionDTO {

    /**
     * 动作类型
     */
    private ActionType type;

    /**
     * 下一个问题索引（如果动作是NEXT_QUESTION）
     */
    private Integer nextQuestionIndex;

    /**
     * 面试是否完成
     */
    private boolean interviewCompleted;

    /**
     * 动作类型枚举
     */
    public enum ActionType {
        /**
         * 继续深挖当前问题
         */
        FOLLOW_UP,

        /**
         * 进入下一个问题
         */
        NEXT_QUESTION,

        /**
         * 结束面试
         */
        END_INTERVIEW,

        /**
         * 继续当前问题（默认）
         */
        CONTINUE
    }
}

