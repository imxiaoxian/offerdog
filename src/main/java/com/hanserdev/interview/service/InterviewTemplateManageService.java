package com.hanserdev.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateCreateReqVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateDetailRspVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateQueryReqVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateUpdateReqVO;

import java.util.UUID;

public interface InterviewTemplateManageService {

    /**
     * 创建面试模板及对应计划模板
     */
    UUID createTemplate(InterviewTemplateCreateReqVO reqVO);

    /**
     * 更新面试模板及计划模板
     */
    void updateTemplate(UUID templateId, InterviewTemplateUpdateReqVO reqVO);

    /**
     * 删除面试模板及计划模板（逻辑停用）
     */
    void deleteTemplate(UUID templateId);

    /**
     * 查询面试模板详情
     */
    InterviewTemplateDetailRspVO getTemplate(UUID templateId);

    /**
     * 分页查询面试模板
     */
    IPage<InterviewTemplateDetailRspVO> pageTemplates(InterviewTemplateQueryReqVO reqVO);
}
