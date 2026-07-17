package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExperienceSummaryRspVO {

    private Long id;

    private String companyName;

    private String position;

    private String experienceType;

    private String formattedContent;

    private String source;

    private Integer views;

    private Integer likes;

    private LocalDateTime createdAt;
}
