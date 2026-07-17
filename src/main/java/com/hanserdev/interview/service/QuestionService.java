package com.hanserdev.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.question.*;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {

    /**
     * 创建题目
     * @param reqVO 题目创建请求参数
     * @param currentUser 当前登录用户信息
     * @return 题目ID
     */
    Long createQuestion(QuestionCreateReqVO reqVO, UserSessionDTO currentUser);

    /**
     * 更新题目
     * @param id 题目ID
     * @param reqVO 题目更新请求参数
     * @param currentUser 当前登录用户信息
     */
    void updateQuestion(Long id, QuestionUpdateReqVO reqVO, UserSessionDTO currentUser);

    /**
     * 删除题目
     * @param id 题目ID
     * @param currentUser 当前登录用户信息
     */
    void deleteQuestion(Long id, UserSessionDTO currentUser);

    /**
     * 获取题目详情
     * @param id 题目ID
     * @param currentUser 当前登录用户信息
     * @return 题目详情信息
     */
    QuestionDetailRspVO getQuestion(Long id, UserSessionDTO currentUser);

    /**
     * 分页查询题目列表
     * @param reqVO 题目查询请求参数
     * @param currentUser 当前登录用户信息
     * @return 题目分页列表
     */
    IPage<QuestionSummaryRspVO> pageQuestions(QuestionQueryReqVO reqVO, UserSessionDTO currentUser);

    /**
     * 上传文件批量导入题目
     *
     * @param file 题目文件
     * @param bankId 题库ID
     * @param categoryId 岗位ID或行业ID
     * @param currentUser 当前登录用户信息
     */
    void batchAddQuestionFromFile(MultipartFile file, Long bankId, Long categoryId, UserSessionDTO currentUser);
}
