package com.hanserdev.interview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.domain.dataobject.InterviewExperienceDO;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.interview.ExperiencePageReqVO;
import com.hanserdev.interview.model.vo.interview.ExperienceSummaryRspVO;
import com.hanserdev.interview.service.InterviewExperienceService;
import com.hanserdev.interview.utils.UserSessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/experiences")
@Validated
@RequiredArgsConstructor
public class InterviewExperienceController {

    private final InterviewExperienceService experienceService;

    @GetMapping
    @ApiOperationLog(description = "分页查询面经列表")
    public Response<IPage<ExperienceSummaryRspVO>> pageExperiences(@Valid ExperiencePageReqVO reqVO) {
        return Response.success(experienceService.pageExperiences(reqVO));
    }

    @GetMapping("/{id}")
    @ApiOperationLog(description = "获取面经详情")
    public Response<InterviewExperienceDO> getExperience(@PathVariable("id") Long id) {
        return Response.success(experienceService.getExperience(id));
    }

    @PostMapping
    @ApiOperationLog(description = "创建面经")
    public Response<Long> createExperience(@Valid @RequestBody InterviewExperienceDO experience,
                                         HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        return Response.success(experienceService.createExperience(experience, currentUser));
    }

    @PutMapping("/{id}")
    @ApiOperationLog(description = "更新面经")
    public Response<Void> updateExperience(@PathVariable("id") Long id,
                                          @Valid @RequestBody InterviewExperienceDO experience,
                                          HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        experienceService.updateExperience(id, experience, currentUser);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperationLog(description = "删除面经")
    public Response<Void> deleteExperience(@PathVariable("id") Long id,
                                           HttpSession session) {
        UserSessionDTO currentUser = UserSessionUtils.getRequiredUser(session);
        experienceService.deleteExperience(id, currentUser);
        return Response.success();
    }

    @PostMapping("/crawl")
    @ApiOperationLog(description = "爬取面经")
    public Response<Void> crawlExperience(@RequestParam String companyName,
                                          @RequestParam String sourceUrl) {
        experienceService.crawlAndSave(companyName, sourceUrl);
        return Response.success();
    }
}
