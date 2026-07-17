package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

import java.util.List;

@Data
public class PracticePlanDayRspVO {

    private int day;
    private String theme;
    private List<String> tasks;
}
