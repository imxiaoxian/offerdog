package com.hanserdev.interview.controller;

import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.service.impl.SampleQuestionDataSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手动补写「示例题库」与示例题目（无需重启；依赖 interview.seed-sample-questions 仅为启动时自动执行，本接口始终可用）。
 */
@RestController
@RequestMapping("/admin/sample-questions")
@Validated
@RequiredArgsConstructor
public class SampleQuestionSeedAdminController {

    private final SampleQuestionDataSeedService sampleQuestionDataSeedService;

    @PostMapping("/seed")
    @ApiOperationLog(description = "补写示例题库与题目")
    public Response<Void> seedSampleQuestions() {
        sampleQuestionDataSeedService.seedIfNeeded();
        Response<Void> r = Response.success();
        r.setMessage("已执行示例题库种子逻辑（若已有题目则跳过写入，详见服务端日志）");
        return r;
    }
}
