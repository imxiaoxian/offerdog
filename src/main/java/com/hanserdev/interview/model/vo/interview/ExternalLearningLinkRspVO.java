package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

/**
 * 精选公开学习资源链接。
 */
@Data
public class ExternalLearningLinkRspVO {

    private String title;
    private String url;
    private String description;
    /** 归类标签，便于展示 */
    private String topicTag;
}
