package com.hanserdev.interview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.PageResponse;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.vo.question.QuestionSummaryRspVO;
import com.hanserdev.interview.service.FavoriteService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller
 *
 * @Author Zane
 * @CreateTime 2025/11/22 星期六 15:50
 */
@RestController
@Slf4j
@RequestMapping("/favorites")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @PostMapping("/add/{questionId}")
    @ApiOperationLog(description = "收藏题目")
    public Response<Object> addFavorite(@PathVariable Long questionId, HttpSession session) {
        favoriteService.addFavorite(questionId, session);
        return Response.success();
    }

    @PostMapping("/remove/{questionId}")
    @ApiOperationLog(description = "取消收藏")
    public Response<Object> removeFavorite(@PathVariable Long questionId, HttpSession session) {
        favoriteService.removeFavorite(questionId, session);
        return Response.success();
    }

    @PostMapping("/list")
    @ApiOperationLog(description = "获取用户收藏的题目列表")
    public PageResponse<QuestionSummaryRspVO> pageFavorite(@RequestParam Integer pageNum, @RequestParam Integer pageSize, HttpSession session) {
        IPage<QuestionSummaryRspVO> pageResult = favoriteService.pageFavorites(session, pageNum, pageSize);
        return PageResponse.success(pageResult.getRecords(), pageResult.getCurrent(), pageResult.getTotal(), pageResult.getSize());
    }
}
