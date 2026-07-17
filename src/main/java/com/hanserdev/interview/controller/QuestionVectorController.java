package com.hanserdev.interview.controller;

import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.service.QuestionVectorService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 题目向量化
 *
 * @Author Zane
 * @CreateTime 2025/11/19 星期三 0:22
 */
@RestController
@RequestMapping("/admin/vector")
@Validated
@RequiredArgsConstructor
public class QuestionVectorController {

    @Resource
    private QuestionVectorService questionVectorService;

    /**
     * 索引单个题目到向量库
     */
    @PostMapping("/{questionId}/index")
    @ApiOperationLog(description = "索引题目到向量库")
    public Response<Void> indexQuestion(@PathVariable Long questionId) {
        questionVectorService.addQuestion(questionId);
        return Response.success();
    }

    /**
     * 批量索引题库到向量数据库
     */
    @PostMapping("/banks/{bankId}/index")
    @ApiOperationLog(description = "批量索引题库到向量库")
    public Response<Integer> indexQuestionBank(@PathVariable Long bankId) {
        int count = questionVectorService.addQuestionBank(bankId);
        Response<Integer> response = Response.success(count);
        response.setMessage(String.format("成功索引 %d 道题目", count));
        return response;
    }

    /**
     * 重建向量数据库索引
     */
    @PostMapping("/index/rebuild")
    @ApiOperationLog(description = "重建向量数据库索引")
    public Response<Integer> rebuildIndex() {
        int count = questionVectorService.rebuildVectorStore();
        Response<Integer> response = Response.success(count);
        response.setMessage(String.format("索引重建完成，共索引 %d 道题目", count));
        return response;
    }

    /**
     * 从向量库删除题目
     */
    @DeleteMapping("/{questionId}/index")
    @ApiOperationLog(description = "从向量库删除题目")
    public Response<Void> deleteQuestionFromIndex(@PathVariable Long questionId) {
        questionVectorService.deleteQuestion(questionId);
        return Response.success();
    }
}