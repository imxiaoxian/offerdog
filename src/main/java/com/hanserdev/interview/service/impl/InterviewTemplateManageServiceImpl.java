package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.hanserdev.interview.domain.dataobject.InterviewPlanTemplateDO;
import com.hanserdev.interview.domain.dataobject.InterviewTemplateDO;
import com.hanserdev.interview.domain.mapper.InterviewPlanTemplateMapper;
import com.hanserdev.interview.domain.mapper.InterviewTemplateMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.InterviewTemplateAssembler;
import com.hanserdev.interview.model.dto.template.PlanStructureDTO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateCreateReqVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateDetailRspVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateQueryReqVO;
import com.hanserdev.interview.model.vo.interview.template.InterviewTemplateUpdateReqVO;
import com.hanserdev.interview.service.InterviewTemplateManageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterviewTemplateManageServiceImpl implements InterviewTemplateManageService {

    private static final String DEFAULT_CONFIG_JSON = """
            {
              "enable_follow_up": true,
              "max_depth_per_question": 3,
              "time_warning_enabled": true
            }
            """;

    private final InterviewTemplateMapper templateMapper;
    private final InterviewPlanTemplateMapper planTemplateMapper;
    private final ObjectMapper objectMapper;
    private final ObjectMapper planStructureMapper;

    public InterviewTemplateManageServiceImpl(InterviewTemplateMapper templateMapper,
                                              InterviewPlanTemplateMapper planTemplateMapper,
                                              ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.planTemplateMapper = planTemplateMapper;
        this.objectMapper = objectMapper;
        this.planStructureMapper = objectMapper.copy();
        this.planStructureMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UUID createTemplate(InterviewTemplateCreateReqVO reqVO) {
        UUID templateId = UUID.randomUUID();
        InterviewTemplateDO templateDO = buildTemplateDO(reqVO, templateId);
        if (templateMapper.insert(templateDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        UUID planTemplateId = UUID.randomUUID();
        InterviewPlanTemplateDO planTemplateDO = buildPlanTemplateDO(reqVO, templateId, planTemplateId);
        if (planTemplateMapper.insert(planTemplateDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        return templateId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(UUID templateId, InterviewTemplateUpdateReqVO reqVO) {
        InterviewTemplateDO templateDO = loadTemplate(templateId);
        mergeTemplate(templateDO, reqVO);
        if (templateMapper.updateById(templateDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        InterviewPlanTemplateDO planTemplateDO = loadPlanTemplate(templateId);
        mergePlanTemplate(planTemplateDO, reqVO);
        if (planTemplateMapper.updateById(planTemplateDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(UUID templateId) {
        InterviewTemplateDO templateDO = loadTemplate(templateId);
        InterviewPlanTemplateDO planTemplateDO = loadPlanTemplate(templateId);

        templateDO.setIsActive(false);
        planTemplateDO.setIsActive(false);

        if (templateMapper.updateById(templateDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        if (planTemplateMapper.updateById(planTemplateDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewTemplateDetailRspVO getTemplate(UUID templateId) {
        InterviewTemplateDO templateDO = loadTemplate(templateId);
        InterviewPlanTemplateDO planTemplateDO = loadPlanTemplate(templateId);
        PlanStructureDTO planStructureDTO = convertToPlanStructureDTO(planTemplateDO.getPlanStructure());
        return InterviewTemplateAssembler.toDetailRspVO(templateDO, planTemplateDO, planStructureDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<InterviewTemplateDetailRspVO> pageTemplates(InterviewTemplateQueryReqVO reqVO) {
        long pageNo = reqVO.getPageNo() == null ? 1L : reqVO.getPageNo();
        long pageSize = reqVO.getPageSize() == null ? 10L : reqVO.getPageSize();
        Page<InterviewTemplateDO> page = new Page<>(pageNo, pageSize);
        Page<InterviewTemplateDO> templatePage = templateMapper.selectPage(page, buildTemplateQuery(reqVO));

        Page<InterviewTemplateDetailRspVO> rspPage =
                new Page<>(templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());

        if (templatePage.getRecords().isEmpty()) {
            rspPage.setRecords(Collections.emptyList());
            return rspPage;
        }

        Map<UUID, InterviewPlanTemplateDO> planTemplateMap = listPlanTemplates(templatePage.getRecords());
        Map<UUID, PlanStructureDTO> structureMap = planTemplateMap.values().stream()
                .collect(Collectors.toMap(InterviewPlanTemplateDO::getTemplateId,
                        item -> convertToPlanStructureDTO(item.getPlanStructure()), (a, b) -> a));

        rspPage.setRecords(templatePage.getRecords().stream()
                .map(item -> InterviewTemplateAssembler.toDetailRspVO(
                        item,
                        planTemplateMap.get(item.getTemplateId()),
                        structureMap.get(item.getTemplateId())
                ))
                .collect(Collectors.toList()));
        return rspPage;
    }

    private InterviewTemplateDO buildTemplateDO(InterviewTemplateCreateReqVO reqVO, UUID templateId) {
        InterviewTemplateDO templateDO = new InterviewTemplateDO();
        templateDO.setTemplateId(templateId);
        templateDO.setTemplateName(reqVO.getTemplateName());
        templateDO.setDescription(reqVO.getDescription());
        templateDO.setCategory(reqVO.getCategory());
        templateDO.setDifficultyLevel(reqVO.getDifficultyLevel());
        templateDO.setEstimatedDurationMinutes(
                reqVO.getEstimatedDurationMinutes() == null ? 30 : reqVO.getEstimatedDurationMinutes());
        templateDO.setSystemPrompt(reqVO.getSystemPrompt());
        templateDO.setDefaultConfig(resolveDefaultConfig(reqVO.getDefaultConfig()));
        templateDO.setIsActive(reqVO.getIsActive() == null || reqVO.getIsActive());
        templateDO.setVersion(reqVO.getVersion() == null ? 1 : reqVO.getVersion());
        return templateDO;
    }

    private InterviewPlanTemplateDO buildPlanTemplateDO(InterviewTemplateCreateReqVO reqVO,
                                                        UUID templateId,
                                                        UUID planTemplateId) {
        InterviewPlanTemplateDO planTemplateDO = new InterviewPlanTemplateDO();
        planTemplateDO.setPlanTemplateId(planTemplateId);
        planTemplateDO.setTemplateId(templateId);
        planTemplateDO.setPlanName(reqVO.getPlanName());
        planTemplateDO.setPlanStructure(convertToPlanStructureNode(reqVO.getPlanStructure()));
        planTemplateDO.setUsageCount(reqVO.getUsageCount() == null ? 0 : reqVO.getUsageCount());
        planTemplateDO.setAvgCompletionRate(reqVO.getAvgCompletionRate());
        planTemplateDO.setIsActive(reqVO.getPlanIsActive() == null || reqVO.getPlanIsActive());
        return planTemplateDO;
    }

    private void mergeTemplate(InterviewTemplateDO templateDO, InterviewTemplateUpdateReqVO reqVO) {
        if (StringUtils.isNotBlank(reqVO.getTemplateName())) {
            templateDO.setTemplateName(reqVO.getTemplateName());
        }
        if (reqVO.getDescription() != null) {
            templateDO.setDescription(reqVO.getDescription());
        }
        if (StringUtils.isNotBlank(reqVO.getCategory())) {
            templateDO.setCategory(reqVO.getCategory());
        }
        if (StringUtils.isNotBlank(reqVO.getDifficultyLevel())) {
            templateDO.setDifficultyLevel(reqVO.getDifficultyLevel());
        }
        if (reqVO.getEstimatedDurationMinutes() != null) {
            templateDO.setEstimatedDurationMinutes(reqVO.getEstimatedDurationMinutes());
        }
        if (StringUtils.isNotBlank(reqVO.getSystemPrompt())) {
            templateDO.setSystemPrompt(reqVO.getSystemPrompt());
        }
        if (reqVO.getDefaultConfig() != null) {
            templateDO.setDefaultConfig(reqVO.getDefaultConfig());
        }
        if (reqVO.getIsActive() != null) {
            templateDO.setIsActive(reqVO.getIsActive());
        }
        if (reqVO.getVersion() != null) {
            templateDO.setVersion(reqVO.getVersion());
        }
    }

    private void mergePlanTemplate(InterviewPlanTemplateDO planTemplateDO, InterviewTemplateUpdateReqVO reqVO) {
        if (StringUtils.isNotBlank(reqVO.getPlanName())) {
            planTemplateDO.setPlanName(reqVO.getPlanName());
        }
        if (reqVO.getPlanStructure() != null) {
            planTemplateDO.setPlanStructure(convertToPlanStructureNode(reqVO.getPlanStructure()));
        }
        if (reqVO.getUsageCount() != null) {
            planTemplateDO.setUsageCount(reqVO.getUsageCount());
        }
        if (reqVO.getAvgCompletionRate() != null) {
            planTemplateDO.setAvgCompletionRate(reqVO.getAvgCompletionRate());
        }
        if (reqVO.getPlanIsActive() != null) {
            planTemplateDO.setIsActive(reqVO.getPlanIsActive());
        }
    }

    private InterviewTemplateDO loadTemplate(UUID templateId) {
        LambdaQueryWrapper<InterviewTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplateDO::getTemplateId, templateId);
        InterviewTemplateDO templateDO = templateMapper.selectOne(wrapper);
        if (templateDO == null) {
            throw new ApiException(ResponseCodeEnum.INTERVIEW_TEMPLATE_NOT_FOUND);
        }
        return templateDO;
    }

    private InterviewPlanTemplateDO loadPlanTemplate(UUID templateId) {
        LambdaQueryWrapper<InterviewPlanTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewPlanTemplateDO::getTemplateId, templateId);
        InterviewPlanTemplateDO planTemplateDO = planTemplateMapper.selectOne(wrapper);
        if (planTemplateDO == null) {
            throw new ApiException(ResponseCodeEnum.INTERVIEW_PLAN_TEMPLATE_NOT_FOUND);
        }
        return planTemplateDO;
    }

    private LambdaQueryWrapper<InterviewTemplateDO> buildTemplateQuery(InterviewTemplateQueryReqVO reqVO) {
        LambdaQueryWrapper<InterviewTemplateDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(reqVO.getTemplateName())) {
            wrapper.like(InterviewTemplateDO::getTemplateName, reqVO.getTemplateName());
        }
        if (StringUtils.isNotBlank(reqVO.getCategory())) {
            wrapper.eq(InterviewTemplateDO::getCategory, reqVO.getCategory());
        }
        if (StringUtils.isNotBlank(reqVO.getDifficultyLevel())) {
            wrapper.eq(InterviewTemplateDO::getDifficultyLevel, reqVO.getDifficultyLevel());
        }
        if (reqVO.getIsActive() != null) {
            wrapper.eq(InterviewTemplateDO::getIsActive, reqVO.getIsActive());
        }
        wrapper.orderByDesc(InterviewTemplateDO::getCreatedAt);
        return wrapper;
    }

    private Map<UUID, InterviewPlanTemplateDO> listPlanTemplates(List<InterviewTemplateDO> templates) {
        List<UUID> templateIds = templates.stream()
                .map(InterviewTemplateDO::getTemplateId)
                .toList();
        if (templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<InterviewPlanTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(InterviewPlanTemplateDO::getTemplateId, templateIds);
        List<InterviewPlanTemplateDO> planTemplates = planTemplateMapper.selectList(wrapper);
        return planTemplates.stream()
                .collect(Collectors.toMap(InterviewPlanTemplateDO::getTemplateId, Function.identity(), (a, b) -> a));
    }

    private JsonNode resolveDefaultConfig(JsonNode defaultConfig) {
        if (defaultConfig != null) {
            return defaultConfig;
        }
        try {
            return objectMapper.readTree(DEFAULT_CONFIG_JSON);
        } catch (Exception e) {
            log.error("解析默认配置失败", e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    private JsonNode convertToPlanStructureNode(PlanStructureDTO planStructure) {
        try {
            return planStructureMapper.valueToTree(planStructure);
        } catch (Exception e) {
            log.error("序列化面试计划结构失败", e);
            throw new ApiException(ResponseCodeEnum.PARAM_ERROR);
        }
    }

    private PlanStructureDTO convertToPlanStructureDTO(JsonNode planStructureNode) {
        if (planStructureNode == null || planStructureNode.isNull()) {
            return null;
        }
        try {
            return planStructureMapper.convertValue(planStructureNode, PlanStructureDTO.class);
        } catch (Exception e) {
            log.error("反序列化面试计划结构失败", e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }
}
