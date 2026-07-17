package com.hanserdev.interview.model.vo.aichat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AiChatRspVO {

    /**
     * 响应内容
     */
    private String v;
}
