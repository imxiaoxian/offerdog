package com.hanserdev.interview.model.convert;

import com.hanserdev.interview.domain.dataobject.QuestionBankDO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankDetailRspVO;

public final class QuestionBankAssembler {

    private QuestionBankAssembler() {
    }

    public static QuestionBankDetailRspVO toDetailRspVO(QuestionBankDO questionBankDO) {
        QuestionBankDetailRspVO rspVO = new QuestionBankDetailRspVO();
        rspVO.setId(questionBankDO.getId());
        rspVO.setName(questionBankDO.getName());
        rspVO.setDescription(questionBankDO.getDescription());
        rspVO.setCategoryId(questionBankDO.getCategoryId());
        rspVO.setCreatedBy(questionBankDO.getCreatedBy());
        rspVO.setCreatedAt(questionBankDO.getCreatedAt());
        rspVO.setUpdatedAt(questionBankDO.getUpdatedAt());
        return rspVO;
    }
}
