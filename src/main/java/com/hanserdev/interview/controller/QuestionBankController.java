package com.hanserdev.interview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.PageResponse;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankCreateReqVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankDetailRspVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankQueryReqVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankUpdateReqVO;
import com.hanserdev.interview.service.QuestionBankService;
import com.hanserdev.interview.utils.UserSessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/question-banks")
@Validated
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @PostMapping
    @ApiOperationLog(description = "创建题库")
    public Response<Long> createQuestionBank(@Valid @RequestBody QuestionBankCreateReqVO reqVO,
                                             HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        return Response.success(questionBankService.createQuestionBank(reqVO, currentUser));
    }

    @PutMapping("/{id}")
    @ApiOperationLog(description = "更新题库")
    public Response<Void> updateQuestionBank(@PathVariable("id") Long id,
                                             @Valid @RequestBody QuestionBankUpdateReqVO reqVO,
                                             HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        questionBankService.updateQuestionBank(id, reqVO, currentUser);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperationLog(description = "删除题库")
    public Response<Void> deleteQuestionBank(@PathVariable("id") Long id,
                                             HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        questionBankService.deleteQuestionBank(id, currentUser);
        return Response.success();
    }

    @GetMapping("/{id}")
    @ApiOperationLog(description = "查询题库详情")
    public Response<QuestionBankDetailRspVO> getQuestionBank(@PathVariable("id") Long id,
                                                             HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        return Response.success(questionBankService.getQuestionBank(id, currentUser));
    }

    @GetMapping
    @ApiOperationLog(description = "分页查询题库")
    public PageResponse<QuestionBankDetailRspVO> pageQuestionBank(@Valid QuestionBankQueryReqVO reqVO,
                                                                 HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        IPage<QuestionBankDetailRspVO> page = questionBankService.pageQuestionBank(reqVO, currentUser);
        return PageResponse.success(page.getRecords(), page.getCurrent(), page.getTotal(), page.getSize());
    }
}
