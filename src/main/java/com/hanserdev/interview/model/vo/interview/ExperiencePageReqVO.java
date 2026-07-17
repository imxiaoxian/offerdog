package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

@Data
public class ExperiencePageReqVO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String companyName;

    private String position;

    private String experienceType;
}
