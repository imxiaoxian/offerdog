package com.hanserdev.interview.utils;

import com.hanserdev.interview.constants.SessionConstants;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import jakarta.servlet.http.HttpSession;

public final class UserSessionUtils {

    private UserSessionUtils() {
    }

    public static UserSessionDTO getRequiredUser(HttpSession session) {
        Object sessionObj = session.getAttribute(SessionConstants.CURRENT_USER); //CURRENT_USER为inai:session:user
        if (sessionObj instanceof UserSessionDTO userSessionDTO) {
            return userSessionDTO;
        }
        throw new ApiException(ResponseCodeEnum.UNAUTHORIZED);
    }
}
