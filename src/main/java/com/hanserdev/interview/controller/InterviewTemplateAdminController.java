package com.hanserdev.interview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.PageResponse;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateCreateReqVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateDetailRspVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateQueryReqVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateUpdateReqVO;
import com.hanserdev.interview.service.InterviewTemplateManageService;
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

import java.util.UUID;

@RestController
@RequestMapping("/admin/interview-templates")
@Validated
@RequiredArgsConstructor
public class InterviewTemplateAdminController {

    private final InterviewTemplateManageService templateManageService;

    @PostMapping
    @ApiOperationLog(description = "创建面试模板及计划模板")
    public Response<UUID> createTemplate(@Valid @RequestBody InterviewTemplateCreateReqVO reqVO,
                                         HttpSession session) {
        assertAdmin(session);
        return Response.success(templateManageService.createTemplate(reqVO));
    }

    @PutMapping("/{templateId}")
    @ApiOperationLog(description = "更新面试模板及计划模板")
    public Response<Void> updateTemplate(@PathVariable("templateId") UUID templateId,
                                         @Valid @RequestBody InterviewTemplateUpdateReqVO reqVO,
                                         HttpSession session) {
        assertAdmin(session);
        templateManageService.updateTemplate(templateId, reqVO);
        return Response.success();
    }

    @DeleteMapping("/{templateId}")
    @ApiOperationLog(description = "删除面试模板及计划模板")
    public Response<Void> deleteTemplate(@PathVariable("templateId") UUID templateId,
                                         HttpSession session) {
        assertAdmin(session);
        templateManageService.deleteTemplate(templateId);
        return Response.success();
    }

    @GetMapping("/{templateId}")
    @ApiOperationLog(description = "查询面试模板详情")
    public Response<InterviewTemplateDetailRspVO> getTemplate(@PathVariable("templateId") UUID templateId,
                                                              HttpSession session) {
        assertAdmin(session);
        return Response.success(templateManageService.getTemplate(templateId));
    }

    @GetMapping
    @ApiOperationLog(description = "分页查询面试模板")
    public PageResponse<InterviewTemplateDetailRspVO> pageTemplates(@Valid InterviewTemplateQueryReqVO reqVO,
                                                                    HttpSession session) {
        assertAdmin(session);
        IPage<InterviewTemplateDetailRspVO> page = templateManageService.pageTemplates(reqVO);
        return PageResponse.success(page.getRecords(), page.getCurrent(), page.getTotal(), page.getSize());
    }

    private void assertAdmin(HttpSession session) {
        UserSessionDTO user = UserSessionUtils.getRequiredUser(session);
        if (user.getRole() != UserRoleEnum.ADMIN) {
            throw new ApiException(ResponseCodeEnum.USER_ROLE_INVALID);
        }
    }
}
