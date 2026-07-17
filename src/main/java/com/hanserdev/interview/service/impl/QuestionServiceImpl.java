package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserdev.interview.domain.dataobject.QuestionBankDO;
import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.domain.mapper.QuestionBankMapper;
import com.hanserdev.interview.domain.mapper.QuestionMapper;
import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import com.hanserdev.interview.enums.QuestionSourceEnum;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.QuestionAssembler;
import com.hanserdev.interview.model.dto.question.QuestionStatsDTO;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.question.*;
import com.hanserdev.interview.service.QuestionService;
import com.hanserdev.interview.service.InterviewStructuredExtractService;
import com.hanserdev.interview.utils.FileUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Resource
    private FileUtils fileUtils;

    @Resource
    private InterviewStructuredExtractService interviewStructuredExtractService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestion(QuestionCreateReqVO reqVO, UserSessionDTO currentUser) {
        QuestionBankDO bank = loadAccessibleBank(reqVO.getBankId(), currentUser);
        QuestionDO questionDO = new QuestionDO();
        questionDO.setBankId(bank.getId());
        questionDO.setCategoryId(reqVO.getCategoryId());
        questionDO.setContent(reqVO.getContent());
        questionDO.setAnswer(reqVO.getAnswer());
        questionDO.setTips(reqVO.getTips());
        questionDO.setDifficulty(reqVO.getDifficulty());
        questionDO.setSource(resolveSource(currentUser));
        questionDO.setTags(convertTags(reqVO.getTags()));
        questionDO.setRemark(reqVO.getRemark());
        questionDO.setCreatedBy(currentUser.getUserId());
        questionDO.setStats(defaultStats());
        if (questionMapper.insert(questionDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        return questionDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(Long id, QuestionUpdateReqVO reqVO, UserSessionDTO currentUser) {
        QuestionDO questionDO = loadAccessibleQuestion(id, currentUser);
        if (reqVO.getBankId() != null && !reqVO.getBankId().equals(questionDO.getBankId())) {
            QuestionBankDO bank = loadAccessibleBank(reqVO.getBankId(), currentUser);
            questionDO.setBankId(bank.getId());
        }
        if (reqVO.getCategoryId() != null) {
            questionDO.setCategoryId(reqVO.getCategoryId());
        }
        if (StringUtils.isNotBlank(reqVO.getContent())) {
            questionDO.setContent(reqVO.getContent());
        }
        if (reqVO.getAnswer() != null) {
            questionDO.setAnswer(reqVO.getAnswer());
        }
        if (reqVO.getTips() != null) {
            questionDO.setTips(reqVO.getTips());
        }
        if (reqVO.getDifficulty() != null) {
            questionDO.setDifficulty(reqVO.getDifficulty());
        }
        if (reqVO.getTags() != null) {
            questionDO.setTags(convertTags(reqVO.getTags()));
        }
        if (reqVO.getRemark() != null) {
            questionDO.setRemark(reqVO.getRemark());
        }
        if (questionMapper.updateById(questionDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long id, UserSessionDTO currentUser) {
        QuestionDO questionDO = loadAccessibleQuestion(id, currentUser);
        questionDO.setDeletedAt(LocalDateTime.now());
        if (questionMapper.updateById(questionDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDetailRspVO getQuestion(Long id, UserSessionDTO currentUser) {
        QuestionDO questionDO = loadAccessibleQuestion(id, currentUser);
        return QuestionAssembler.toDetailRspVO(questionDO);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<QuestionSummaryRspVO> pageQuestions(QuestionQueryReqVO reqVO, UserSessionDTO currentUser) {
        long pageNo = reqVO.getPageNo() == null ? 1L : reqVO.getPageNo();
        long pageSize = reqVO.getPageSize() == null ? 10L : reqVO.getPageSize();
        Page<QuestionDO> page = new Page<>(pageNo, pageSize);
        Page<QuestionDO> questionPage = questionMapper.selectPage(page, buildQueryWrapper(reqVO, currentUser));
        Page<QuestionSummaryRspVO> rspPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        rspPage.setRecords(questionPage.getRecords().stream()
                .map(QuestionAssembler::toSummaryRspVO)
                .collect(Collectors.toList()));
        return rspPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionFromFile(MultipartFile file, Long bankId, Long categoryId, UserSessionDTO currentUser) {
        // 验证用户是否有权限访问该题库
        loadAccessibleBank(bankId, currentUser);
        
        List<QuestionDO> questionDOList = extractQuestionDOFromJsonFile(file);

        if (questionDOList != null && !questionDOList.isEmpty()) {
            // 设置必要的字段
            for (QuestionDO question : questionDOList) {
                if (question.getBankId() == null) {
                    question.setBankId(bankId);
                }
                if (question.getCategoryId() == null) {
                    question.setCategoryId(categoryId);
                }
                if (question.getSource() == null) {
                    question.setSource(resolveSource(currentUser));
                }
                if (question.getStats() == null) {
                    question.setStats(defaultStats());
                }
                if (question.getCreatedBy() == null) {
                    question.setCreatedBy(currentUser.getUserId());
                }

            }
            // 批量插入
            questionMapper.insert(questionDOList);
            log.info("批量添加题目成功: 共{}道题", questionDOList.size());
        }
    }

    /**
     * 从JSON文件中提取题目信息
     *
     * @param file JSON文件
     * @return 提取的题目信息
     */
    private List<QuestionDO> extractQuestionDOFromJsonFile(MultipartFile file) {
        String uniqueFilename = fileUtils.generateUniqueFilename(file);
        String fileUrL = fileUtils.uploadFile(file, uniqueFilename);
        String extractionInstruction = """
                请从文档中提取所有的面试题目信息。对于每个题目，提取以下内容：
                - content: 题目的完整内容
                - answer: 题目的标准答案
                - tips: 解答提示或思路
                - difficulty: 题目难度（easy、medium、hard 中的一个，默认为 medium）
                - tags: 题目相关的标签，用逗号分隔的字符串数组
                - source: 题目来源（根据创建者自动确认）
                - remark: 任何额外的备注信息
                
                如果某些字段在文档中不存在，请使用合理的默认值或留空。""";
        try {
            return interviewStructuredExtractService.extractFromDocument(fileUrL, QuestionDO.class, extractionInstruction);
        } catch (InterviewStructuredExtractService.ExtractionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private QuestionDO loadAccessibleQuestion(Long id, UserSessionDTO currentUser) {
        QueryWrapper<QuestionDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.isNull("deleted_at");
        appendQuestionAccessCondition(wrapper, currentUser);
        QuestionDO questionDO = questionMapper.selectOne(wrapper);
        if (questionDO == null) {
            throw new ApiException(ResponseCodeEnum.QUESTION_NOT_FOUND);
        }
        return questionDO;
    }

    private QuestionBankDO loadAccessibleBank(Long bankId, UserSessionDTO currentUser) {
        QueryWrapper<QuestionBankDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", bankId);
        wrapper.isNull("deleted_at");
        if (!isAdmin(currentUser)) {
            // 与 QuestionBankService 列表/详情一致：官方 + 本人
            wrapper.and(i -> i.isNull("created_by")
                    .or(j -> j.eq("created_by", currentUser.getUserId())));
        }
        QuestionBankDO bankDO = questionBankMapper.selectOne(wrapper);
        if (bankDO == null) {
            throw new ApiException(ResponseCodeEnum.QUESTION_BANK_NOT_FOUND);
        }
        return bankDO;
    }

    private QueryWrapper<QuestionDO> buildQueryWrapper(QuestionQueryReqVO reqVO, UserSessionDTO currentUser) {
        QueryWrapper<QuestionDO> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");
        appendQuestionAccessCondition(wrapper, currentUser);
        if (StringUtils.isNotBlank(reqVO.getKeyword())) {
            wrapper.like("content", reqVO.getKeyword());
        }
        if (StringUtils.isNotBlank(reqVO.getAnswerKeyword())) {
            wrapper.like("answer", reqVO.getAnswerKeyword());
        }
        if (reqVO.getDifficulty() != null) {
            applyDifficultyCondition(wrapper, reqVO.getDifficulty());
        }
        if (CollectionUtils.isNotEmpty(reqVO.getTags())) {
            // 1. 只取第一个标签（因为你确认一次只搜索一个标签）
            String singleTag = reqVO.getTags().getFirst();
            // 2. 构造模糊搜索项，例如将 "Java" 变为 "%Java%"
            String fuzzyTerm = "%" + singleTag + "%";
            // 3. 使用 PostgreSQL 的 EXISTS 结合 unnest 进行模糊匹配
            // SQL 表达式：EXISTS (SELECT 1 FROM unnest(tags) AS t WHERE t LIKE {0})
            wrapper.apply("EXISTS (SELECT 1 FROM unnest(tags) AS t WHERE t LIKE {0})", fuzzyTerm);
        }
        if (reqVO.getBankId() != null) {
            wrapper.eq("bank_id", reqVO.getBankId());
        }
        if (CollectionUtils.isNotEmpty(reqVO.getCategoryIds())) {
            wrapper.in("category_id", reqVO.getCategoryIds());
        } else if (reqVO.getCategoryId() != null) {
            wrapper.eq("category_id", reqVO.getCategoryId());
        }
        wrapper.orderByDesc("created_at");
        return wrapper;
    }

    private void appendQuestionAccessCondition(QueryWrapper<QuestionDO> wrapper, UserSessionDTO currentUser) {
        if (isAdmin(currentUser)) {
            // 管理员浏览题库题目时不限制 source，避免漏掉用户贡献题
            return;
        }
        // 普通用户：官方题目 + 本人创建的题目
        wrapper.and(i -> i.apply("source = CAST({0} AS question_source)", QuestionSourceEnum.OFFICIAL.getDbValue())
                .or(j -> j.apply("source = CAST({0} AS question_source)", QuestionSourceEnum.USER.getDbValue())
                        .eq("created_by", currentUser.getUserId())));
    }

    private QuestionSourceEnum resolveSource(UserSessionDTO currentUser) {
        return isAdmin(currentUser) ? QuestionSourceEnum.OFFICIAL : QuestionSourceEnum.USER;
    }

    private boolean isAdmin(UserSessionDTO currentUser) {
        return currentUser.getRole() == UserRoleEnum.ADMIN;
    }

    private void applySourceCondition(QueryWrapper<QuestionDO> wrapper, QuestionSourceEnum sourceEnum) {
        wrapper.apply("source = CAST({0} AS question_source)", sourceEnum.getDbValue());
    }

    private void applyDifficultyCondition(QueryWrapper<QuestionDO> wrapper, QuestionDifficultyEnum difficultyEnum) {
        wrapper.apply("difficulty = CAST({0} AS question_difficulty)", difficultyEnum.getDbValue());
    }

    private String[] convertTags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return new String[0];
        }
        return tags.toArray(new String[0]);
    }

    private QuestionStatsDTO defaultStats() {
        QuestionStatsDTO stats = new QuestionStatsDTO();
        stats.setViews(0);
        stats.setLikes(0);
        stats.setFavorites(0);
        return stats;
    }
}
