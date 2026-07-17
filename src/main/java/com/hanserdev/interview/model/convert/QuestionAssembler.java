package com.hanserdev.interview.model.convert;

import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.model.vo.question.QuestionDetailRspVO;
import com.hanserdev.interview.model.vo.question.QuestionSummaryRspVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class QuestionAssembler {

    private QuestionAssembler() {
    }

    public static QuestionDetailRspVO toDetailRspVO(QuestionDO questionDO) {
        QuestionDetailRspVO rspVO = new QuestionDetailRspVO();
        rspVO.setId(questionDO.getId());
        rspVO.setBankId(questionDO.getBankId());
        rspVO.setCategoryId(questionDO.getCategoryId());
        rspVO.setContent(questionDO.getContent());
        rspVO.setAnswer(questionDO.getAnswer());
        rspVO.setTips(questionDO.getTips());
        rspVO.setDifficulty(questionDO.getDifficulty());
        rspVO.setSource(questionDO.getSource());
        rspVO.setTags(convertTags(questionDO.getTags()));
        rspVO.setStats(questionDO.getStats());
        rspVO.setRemark(questionDO.getRemark());
        rspVO.setCreatedBy(questionDO.getCreatedBy());
        rspVO.setCreatedAt(questionDO.getCreatedAt());
        rspVO.setUpdatedAt(questionDO.getUpdatedAt());
        return rspVO;
    }

    public static QuestionSummaryRspVO toSummaryRspVO(QuestionDO questionDO) {
        QuestionSummaryRspVO rspVO = new QuestionSummaryRspVO();
        rspVO.setId(questionDO.getId());
        rspVO.setBankId(questionDO.getBankId());
        rspVO.setCategoryId(questionDO.getCategoryId());
        rspVO.setContent(questionDO.getContent());
        rspVO.setDifficulty(questionDO.getDifficulty());
        rspVO.setSource(questionDO.getSource());
        rspVO.setTags(convertTags(questionDO.getTags()));
        rspVO.setCreatedAt(questionDO.getCreatedAt());
        return rspVO;
    }

    private static List<String> convertTags(String[] tags) {
        return tags == null ? Collections.emptyList() : Arrays.asList(tags);
    }
}
