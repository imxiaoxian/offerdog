package com.hanserdev.interview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.PageResponse;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.question.*;
import com.hanserdev.interview.service.QuestionService;
import com.hanserdev.interview.utils.UserSessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/questions")
@Validated
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @ApiOperationLog(description = "创建题目")
    public Response<Long> createQuestion(@Valid @RequestBody QuestionCreateReqVO reqVO,
                                         HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        return Response.success(questionService.createQuestion(reqVO, currentUser));
    }

    @PutMapping("/{id}")
    @ApiOperationLog(description = "更新题目")
    public Response<Void> updateQuestion(@PathVariable("id") Long id,
                                         @Valid @RequestBody QuestionUpdateReqVO reqVO,
                                         HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        questionService.updateQuestion(id, reqVO, currentUser);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperationLog(description = "删除题目")
    public Response<Void> deleteQuestion(@PathVariable("id") Long id,
                                         HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        questionService.deleteQuestion(id, currentUser);
        return Response.success();
    }

    @GetMapping("/{id}")
    @ApiOperationLog(description = "查询题目详情")
    public Response<QuestionDetailRspVO> getQuestion(@PathVariable("id") Long id,
                                                     HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        return Response.success(questionService.getQuestion(id, currentUser));
    }

    @GetMapping
    @ApiOperationLog(description = "分页查询题目")
    public PageResponse<QuestionSummaryRspVO> pageQuestions(@Valid QuestionQueryReqVO reqVO,
                                                            HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        IPage<QuestionSummaryRspVO> page = questionService.pageQuestions(reqVO, currentUser);
        return PageResponse.success(page.getRecords(), page.getCurrent(), page.getTotal(), page.getSize());
    }

    @PostMapping("/batch")
    @ApiOperationLog(description = "上传题目文件批量添加题目")
    public Response<Void> batchAddQuestion(@RequestParam("file") MultipartFile file, 
                                           @RequestParam("bankId") Long bankId, 
                                           @RequestParam("categoryId") Long categoryId, 
                                           HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        questionService.batchAddQuestionFromFile(file, bankId, categoryId, currentUser);
        return Response.success();
    }
}
