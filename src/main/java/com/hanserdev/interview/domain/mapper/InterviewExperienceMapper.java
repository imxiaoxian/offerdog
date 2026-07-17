package com.hanserdev.interview.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserdev.interview.domain.dataobject.InterviewExperienceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewExperienceMapper extends BaseMapper<InterviewExperienceDO> {
}
