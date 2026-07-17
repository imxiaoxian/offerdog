package com.hanserdev.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankCreateReqVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankDetailRspVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankQueryReqVO;
import com.hanserdev.interview.model.vo.questionbank.QuestionBankUpdateReqVO;

public interface QuestionBankService {

    /**
     * 创建题库
     * @param reqVO 题库创建请求参数
     * @return 题库ID
     */
    Long createQuestionBank(QuestionBankCreateReqVO reqVO, UserSessionDTO currentUser);

    /**
     * 更新题库
     * @param id 题库ID
     * @param reqVO 题库更新请求参数
     */
    void updateQuestionBank(Long id, QuestionBankUpdateReqVO reqVO, UserSessionDTO currentUser);

    /**
     * 删除题库
     * @param id 题库ID
     */
    void deleteQuestionBank(Long id, UserSessionDTO currentUser);

    /**
     * 获取题库详情
     * @param id 题库ID
     * @return 题库详情信息
     */
    QuestionBankDetailRspVO getQuestionBank(Long id, UserSessionDTO currentUser);

    /**
     * 分页查询题库列表
     * @param reqVO 题库查询请求参数
     * @return 题库分页结果
     */
    IPage<QuestionBankDetailRspVO> pageQuestionBank(QuestionBankQueryReqVO reqVO, UserSessionDTO currentUser);
}
