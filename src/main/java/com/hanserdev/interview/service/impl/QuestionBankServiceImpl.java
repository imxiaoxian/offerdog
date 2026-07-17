package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserdev.interview.domain.dataobject.CategoryDO;
import com.hanserdev.interview.domain.dataobject.QuestionBankDO;
import com.hanserdev.interview.domain.mapper.CategoryMapper;
import com.hanserdev.interview.domain.mapper.QuestionBankMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.QuestionBankAssembler;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankCreateReqVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankDetailRspVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankQueryReqVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankUpdateReqVO;
import com.hanserdev.interview.service.QuestionBankService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    @Resource
    private  QuestionBankMapper questionBankMapper;
    @Resource
    private  CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestionBank(QuestionBankCreateReqVO reqVO, UserSessionDTO currentUser) {
        validateCategoryIsJob(reqVO.getCategoryId());
        QuestionBankDO questionBankDO = new QuestionBankDO();
        questionBankDO.setName(reqVO.getName());
        questionBankDO.setDescription(reqVO.getDescription());
        questionBankDO.setCategoryId(reqVO.getCategoryId());
        Long createdBy = resolveCreatedBy(currentUser);
        // 允许普通用户创建自己的题库，管理员创建的为官方题库
        questionBankDO.setCreatedBy(resolveCreatedBy(currentUser));
        if (questionBankMapper.insert(questionBankDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        return questionBankDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionBank(Long id, QuestionBankUpdateReqVO reqVO, UserSessionDTO currentUser) {
        QuestionBankDO questionBankDO = loadAccessibleQuestionBank(id, currentUser);
        if (StringUtils.isNotBlank(reqVO.getName())) {
            questionBankDO.setName(reqVO.getName());
        }
        if (reqVO.getDescription() != null) {
            questionBankDO.setDescription(reqVO.getDescription());
        }
        if (reqVO.getCategoryId() != null && !reqVO.getCategoryId().equals(questionBankDO.getCategoryId())) {
            validateCategoryIsJob(reqVO.getCategoryId());
            questionBankDO.setCategoryId(reqVO.getCategoryId());
        }
        if (questionBankMapper.updateById(questionBankDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestionBank(Long id, UserSessionDTO currentUser) {
        QuestionBankDO questionBankDO = loadAccessibleQuestionBank(id, currentUser);
        questionBankDO.setDeletedAt(LocalDateTime.now());
        if (questionBankMapper.updateById(questionBankDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankDetailRspVO getQuestionBank(Long id, UserSessionDTO currentUser) {
        QuestionBankDO questionBankDO = loadAccessibleQuestionBank(id, currentUser);
        return QuestionBankAssembler.toDetailRspVO(questionBankDO);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<QuestionBankDetailRspVO> pageQuestionBank(QuestionBankQueryReqVO reqVO, UserSessionDTO currentUser) {
        long pageNo = reqVO.getPageNo() == null ? 1L : reqVO.getPageNo();
        long pageSize = reqVO.getPageSize() == null ? 10L : reqVO.getPageSize();
        Page<QuestionBankDO> page = new Page<>(pageNo, pageSize);
        Page<QuestionBankDO> questionBankPage = questionBankMapper.selectPage(page, buildQueryWrapper(reqVO, currentUser));
        Page<QuestionBankDetailRspVO> rspPage = new Page<>(questionBankPage.getCurrent(), questionBankPage.getSize(), questionBankPage.getTotal());
        rspPage.setRecords(questionBankPage.getRecords().stream()
                .map(QuestionBankAssembler::toDetailRspVO)
                .collect(Collectors.toList()));
        return rspPage;
    }

    private QuestionBankDO loadAccessibleQuestionBank(Long id, UserSessionDTO currentUser) {
        QueryWrapper<QuestionBankDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.isNull("deleted_at");
        appendAccessCondition(wrapper, currentUser);
        QuestionBankDO questionBankDO = questionBankMapper.selectOne(wrapper);
        if (questionBankDO == null) {
            throw new ApiException(ResponseCodeEnum.QUESTION_BANK_NOT_FOUND);
        }
        return questionBankDO;
    }

    private void validateCategoryIsJob(Long categoryId) {
        CategoryDO categoryDO = categoryMapper.selectById(categoryId);
        if (categoryDO == null || categoryDO.getDeletedAt() != null) {
            throw new ApiException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }
        if (categoryDO.getLevel() == null || categoryDO.getLevel() != 2) {
            throw new ApiException(ResponseCodeEnum.CATEGORY_LEVEL_INVALID);
        }
    }

    private QueryWrapper<QuestionBankDO> buildQueryWrapper(QuestionBankQueryReqVO reqVO, UserSessionDTO currentUser) {
        QueryWrapper<QuestionBankDO> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");
        appendAccessCondition(wrapper, currentUser);
        if (StringUtils.isNotBlank(reqVO.getName())) {
            wrapper.like("name", reqVO.getName());
        }
        if (CollectionUtils.isNotEmpty(reqVO.getCategoryIds())) {
            wrapper.in("category_id", reqVO.getCategoryIds());
        } else if (reqVO.getCategoryId() != null) {
            wrapper.eq("category_id", reqVO.getCategoryId());
        }
        wrapper.orderByDesc("created_at");
        return wrapper;
    }

    private void appendAccessCondition(QueryWrapper<QuestionBankDO> wrapper, UserSessionDTO currentUser) {
        if (isAdmin(currentUser)) {
            // 管理员可查看全部题库（含用户自建），便于管理与浏览
            return;
        }
        // 普通用户：官方题库 + 本人创建
        wrapper.and(i -> i.isNull("created_by")
                .or(j -> j.eq("created_by", currentUser.getUserId())));
    }

    private Long resolveCreatedBy(UserSessionDTO currentUser) {
        return isAdmin(currentUser) ? null : currentUser.getUserId();
    }

    private boolean isAdmin(UserSessionDTO currentUser) {
        return currentUser.getRole() == UserRoleEnum.ADMIN;
    }
}
