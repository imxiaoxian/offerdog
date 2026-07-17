package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserdev.interview.constants.SessionConstants;
import com.hanserdev.interview.domain.dataobject.FavoriteDO;
import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.domain.dataobject.UserDO;
import com.hanserdev.interview.domain.mapper.FavoriteMapper;
import com.hanserdev.interview.domain.mapper.QuestionMapper;
import com.hanserdev.interview.domain.mapper.UserMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.QuestionAssembler;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.question.QuestionSummaryRspVO;
import com.hanserdev.interview.service.FavoriteService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目收藏业务逻辑实现类
 *
 * @Author Zane
 * @CreateTime 2025/11/22 星期六 14:35
 */
@Service
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long questionId, HttpSession session) {
        // 查询用户信息
        validateUser((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER));

        // 查询题目信息
        validateQuestion(questionId);

        // 判断是否收藏过该题目
        FavoriteDO favoriteDO = favoriteMapper.selectOne(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId()));

        if (favoriteDO != null) {
            Long[] idsArr = favoriteDO.getQuestionIds();
            if (idsArr != null && Arrays.asList(idsArr).contains(questionId)) {
                log.warn("用户[{}]已收藏题目[{}]", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId(), questionId);
                throw new ApiException(ResponseCodeEnum.QUESTION_ALREADY_FAVORITE);
            }
            // 添加到已有的收藏列表
            List<Long> ids = idsArr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(idsArr));
            ids.add(questionId);
            favoriteDO.setQuestionIds(ids.toArray(new Long[0]));
            favoriteMapper.updateById(favoriteDO);
            log.info("用户[{}]成功收藏题目[{}]", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId(), questionId);
        } else {
            // 创建新的收藏记录
            List<Long> ids = new ArrayList<>();
            ids.add(questionId);
            FavoriteDO favorite = new FavoriteDO();
            favorite.setUserId(((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId());
            favorite.setQuestionIds(ids.toArray(new Long[0]));
            favorite.setCreateAt(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            log.info("用户[{}]首次收藏题目[{}]", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId(), questionId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long questionId, HttpSession session) {
        // 查询用户信息
        validateUser((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER));

        // 查询题目信息
        validateQuestion(questionId);

        // 查询用户的收藏记录
        FavoriteDO favoriteDO = favoriteMapper.selectOne(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId()));

        if (favoriteDO == null) {
            log.warn("用户[{}]没有收藏记录", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId());
            return;
        }

        Long[] idsArr = favoriteDO.getQuestionIds();
        List<Long> ids = idsArr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(idsArr));
        if (!ids.contains(questionId)) {
            log.warn("用户[{}]未收藏题目[{}]", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId(), questionId);
            return;
        }

        // 从收藏列表中移除
        ids.remove(questionId);

        if (ids.isEmpty()) {
            // 如果收藏列表为空，删除整条记录
            favoriteMapper.deleteById(favoriteDO.getId());
            log.info("用户[{}]取消收藏题目[{}]，收藏列表已清空", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId(), questionId);
        } else {
            // 更新收藏列表
            favoriteDO.setQuestionIds(ids.toArray(new Long[0]));
            favoriteMapper.updateById(favoriteDO);
            log.info("用户[{}]取消收藏题目[{}]", ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId(), questionId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long questionId, HttpSession session) {
        if (questionId == null || session == null || session.getAttribute(SessionConstants.CURRENT_USER) == null || ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId() == null) {
            return false;
        }

        FavoriteDO favoriteDO = favoriteMapper.selectOne(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId()));

        if (favoriteDO == null) {
            return false;
        }

        Long[] idsArr = favoriteDO.getQuestionIds();
        return idsArr != null && Arrays.asList(idsArr).contains(questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listFavorites(HttpSession session) {
        if (session == null || session.getAttribute(SessionConstants.CURRENT_USER) == null || ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId() == null) {
            return new ArrayList<>();
        }

        FavoriteDO favoriteDO = favoriteMapper.selectOne(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId()));

        Long[] idsArr = favoriteDO == null ? null : favoriteDO.getQuestionIds();
        if (idsArr == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(idsArr));
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<QuestionSummaryRspVO> pageFavorites(HttpSession session, Integer pageNum, Integer pageSize) {
        // 参数校验
        if (session == null || session.getAttribute(SessionConstants.CURRENT_USER) == null || ((UserSessionDTO) session.getAttribute(SessionConstants.CURRENT_USER)).getUserId() == null) {
            return new Page<>(1, 10, 0);
        }
        long size = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100);

        // 获取用户收藏的题目ID列表
        List<Long> favoriteQuestionIds = listFavorites(session);

        // 如果没有收藏，返回空分页
        if (favoriteQuestionIds.isEmpty()) {
            Page<QuestionSummaryRspVO> emptyPage = new Page<>(pageNum, size, 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }

        // 分页查询收藏的题目
        Page<QuestionDO> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<QuestionDO> queryWrapper = new LambdaQueryWrapper<QuestionDO>()
                .in(QuestionDO::getId, favoriteQuestionIds)
                .isNull(QuestionDO::getDeletedAt)
                .orderByDesc(QuestionDO::getCreatedAt);

        Page<QuestionDO> questionPage = questionMapper.selectPage(page, queryWrapper);

        // 转换为VO
        Page<QuestionSummaryRspVO> rspPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        rspPage.setRecords(questionPage.getRecords().stream()
                .map(QuestionAssembler::toSummaryRspVO)
                .collect(Collectors.toList()));

        return rspPage;
    }

    private void validateUser(UserSessionDTO user) {
        if (user == null || user.getUserId() == null) {
            throw new ApiException(ResponseCodeEnum.UNAUTHORIZED);
        }
        UserDO userDO = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getId, user.getUserId())
                .isNull(UserDO::getDeletedAt));
        if (userDO == null) {
            throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
        }
    }

    private void validateQuestion(Long questionId) {
        QuestionDO questionDO = questionMapper.selectOne(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getId, questionId)
                .isNull(QuestionDO::getDeletedAt));
        if (questionDO == null) {
            throw new ApiException(ResponseCodeEnum.QUESTION_NOT_FOUND);
        }
    }
}