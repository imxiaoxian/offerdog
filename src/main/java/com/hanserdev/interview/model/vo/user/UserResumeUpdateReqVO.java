package com.hanserdev.interview.model.vo.user;

import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class UserResumeUpdateReqVO {

    /**
     * 用户简历信息
     */
    @Valid
    private UserResumeDTO resume;
}
