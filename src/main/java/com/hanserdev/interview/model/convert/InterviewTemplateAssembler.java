package com.hanserdev.interview.model.convert;

import com.hanserdev.interview.domain.dataobject.InterviewPlanTemplateDO;
import com.hanserdev.interview.domain.dataobject.InterviewTemplateDO;
import com.hanserdev.interview.model.dto.template.PlanStructureDTO;
import com.hanserdev.interview.model.vo.interview.template.InterviewPlanTemplateRspVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateDetailRspVO;

/**
 * 面试模板及计划模板转换器
 */
public final class InterviewTemplateAssembler {

    private InterviewTemplateAssembler() {
    }

    public static InterviewTemplateDetailRspVO toDetailRspVO(InterviewTemplateDO templateDO,
                                                             InterviewPlanTemplateDO planTemplateDO,
                                                             PlanStructureDTO planStructureDTO) {
        if (templateDO == null) {
            return null;
        }

        InterviewTemplateDetailRspVO rspVO = new InterviewTemplateDetailRspVO();
        rspVO.setTemplateId(templateDO.getTemplateId());
        rspVO.setTemplateName(templateDO.getTemplateName());
        rspVO.setDescription(templateDO.getDescription());
        rspVO.setCategory(templateDO.getCategory());
        rspVO.setDifficultyLevel(templateDO.getDifficultyLevel());
        rspVO.setEstimatedDurationMinutes(templateDO.getEstimatedDurationMinutes());
        rspVO.setSystemPrompt(templateDO.getSystemPrompt());
        rspVO.setDefaultConfig(templateDO.getDefaultConfig());
        rspVO.setIsActive(templateDO.getIsActive());
        rspVO.setVersion(templateDO.getVersion());
        rspVO.setCreatedAt(templateDO.getCreatedAt());
        rspVO.setUpdatedAt(templateDO.getUpdatedAt());

        if (planTemplateDO != null) {
            InterviewPlanTemplateRspVO planVO = new InterviewPlanTemplateRspVO();
            planVO.setPlanTemplateId(planTemplateDO.getPlanTemplateId());
            planVO.setTemplateId(planTemplateDO.getTemplateId());
            planVO.setPlanName(planTemplateDO.getPlanName());
            planVO.setPlanStructure(planStructureDTO);
            planVO.setUsageCount(planTemplateDO.getUsageCount());
            planVO.setAvgCompletionRate(planTemplateDO.getAvgCompletionRate());
            planVO.setIsActive(planTemplateDO.getIsActive());
            planVO.setCreatedAt(planTemplateDO.getCreatedAt());
            planVO.setUpdatedAt(planTemplateDO.getUpdatedAt());
            rspVO.setPlanTemplate(planVO);
        }

        return rspVO;
    }
}
