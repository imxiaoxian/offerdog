package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

import java.util.List;

/**
 * 大模型根据薄弱点生成的个性化周计划（默认 7 天）。
 */
@Data
public class PracticePlanGeneratedRspVO {

    private String goal;
    private List<PracticePlanDayRspVO> days;
}
