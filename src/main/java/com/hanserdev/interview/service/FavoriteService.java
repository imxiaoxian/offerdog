package com.hanserdev.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.model.vo.question.QuestionSummaryRspVO;
import jakarta.servlet.http.HttpSession;

import java.util.List;

/**
 * 业务逻辑层
 *
 * @Author Zane
 * @CreateTime 2025/11/22 星期六 11:48
 */
public interface FavoriteService {

    /**
     * 收藏题目
     *
     * @param questionId 题目ID
     * @param session 当前会话
     */
    void addFavorite(Long questionId, HttpSession session);

    /**
     * 取消收藏
     *
     * @param questionId 题目ID
     * @param session 当前会话
     */
    void removeFavorite(Long questionId, HttpSession session);

    /**
     * 判断用户是否收藏了该题目
     *
     * @param questionId 题目ID
     * @param session 当前会话
     * @return true:已收藏 false:未收藏
     */
    boolean isFavorite(Long questionId, HttpSession session);

    /**
     * 获取用户收藏的题目ID列表
     *
     * @param session 当前会话
     * @return 收藏的题目ID列表
     */
    List<Long> listFavorites(HttpSession session);

    /**
     * 分页获取用户收藏的题目列表
     *
     * @param session 当前会话
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 收藏的题目列表
     */
    IPage<QuestionSummaryRspVO> pageFavorites(HttpSession session, Integer pageNum, Integer pageSize);
}
