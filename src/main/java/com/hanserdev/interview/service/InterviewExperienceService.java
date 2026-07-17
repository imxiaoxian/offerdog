package com.hanserdev.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.domain.dataobject.InterviewExperienceDO;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.interview.ExperiencePageReqVO;
import com.hanserdev.interview.model.vo.interview.ExperienceSummaryRspVO;

public interface InterviewExperienceService {

    Long createExperience(InterviewExperienceDO experience, UserSessionDTO currentUser);

    void updateExperience(Long id, InterviewExperienceDO experience, UserSessionDTO currentUser);

    void deleteExperience(Long id, UserSessionDTO currentUser);

    InterviewExperienceDO getExperience(Long id);

    IPage<ExperienceSummaryRspVO> pageExperiences(ExperiencePageReqVO reqVO);

    void crawlAndSave(String companyName, String sourceUrl);
}
