package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.constants.SessionConstants;
import com.hanserdev.interview.domain.dataobject.UserDO;
import com.hanserdev.interview.domain.mapper.UserMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.UserAssembler;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.vo.user.*;
import com.hanserdev.interview.service.AuthService;
import com.hanserdev.interview.utils.PasswordUtils;
import com.hanserdev.interview.utils.helper.MailHelper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_PHONE = "N/A";

    private final UserMapper userMapper;
    private final MailHelper mailHelper;

    @Override
    public Response<?> sendRegisterCode(UserEmailRegisterCodeSendReqVO reqVO, HttpSession session) {
        String assertUsernameUnique = assertUsernameUnique(reqVO.getUsername());
        if (assertUsernameUnique != null) {
            return Response.fail(assertUsernameUnique);
        }
        String assertEmailAvailable = assertEmailAvailable(reqVO.getEmail());
        if (assertEmailAvailable != null) {
            return Response.fail(assertEmailAvailable);
        }
        boolean success = mailHelper.sendRegistrationCode(reqVO.getUsername(), reqVO.getEmail(), session);
        if (!success) {
            return Response.fail("系统错误！邮件发送失败！");
        }
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginRspVO registerByEmail(UserEmailRegisterReqVO reqVO, HttpSession session) {
        assertUsernameUnique(reqVO.getUsername());
        assertEmailAvailable(reqVO.getEmail());
        boolean codeMatched = mailHelper.verifyRegistrationCode(reqVO.getEmail(), reqVO.getVerificationCode(), session);
        if (!codeMatched) {
            throw new ApiException(ResponseCodeEnum.EMAIL_CODE_INVALID);
        }
        UserDO userDO = buildNewUser(reqVO);
        if (userMapper.insert(userDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        return storeSessionAndBuildRsp(userDO, session);
    }

    @Override
    public Response<?> sendLoginCode(UserEmailLoginCodeSendReqVO reqVO, HttpSession session) {
        UserDO userDO = loadUserByEmail(reqVO.getEmail());
        if (userDO == null) {
            return Response.fail("用户不存在！");
        }
        boolean success = mailHelper.sendLoginCode(reqVO.getEmail(), session);
        if (!success) {
            return Response.fail("邮件发送失败！请检查邮箱是否正确！");
        }
        return Response.success();
    }

    @Override
    public UserLoginRspVO loginByEmailCode(UserEmailLoginByCodeReqVO reqVO, HttpSession session) {
        UserDO userDO = loadUserByEmail(reqVO.getEmail());
        boolean codeMatched = mailHelper.verifyLoginCode(reqVO.getEmail(), reqVO.getVerificationCode(), session);
        if (!codeMatched) {
            throw new ApiException(ResponseCodeEnum.EMAIL_CODE_INVALID);
        }
        return storeSessionAndBuildRsp(userDO, session);
    }

    @Override
    public Response<?> loginByEmailPassword(UserEmailLoginByPasswordReqVO reqVO, HttpSession session) {
        UserDO userDO = loadUserByEmail(reqVO.getEmail());
        if (userDO == null) {
            return Response.fail("用户不存在或密码错误！");
        }
        if (!PasswordUtils.matches(reqVO.getPassword(), userDO.getPasswordHash())) {
            return Response.fail("用户不存在或密码错误！");
        }
        UserLoginRspVO userLoginRspVO = storeSessionAndBuildRsp(userDO, session);
        return Response.success(userLoginRspVO);
    }

    private UserDO buildNewUser(UserEmailRegisterReqVO reqVO) {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setEmail(reqVO.getEmail());
        profile.setPhone(DEFAULT_PHONE);
        profile.setRealName(reqVO.getUsername());

        UserDO userDO = new UserDO();
        userDO.setUsername(reqVO.getUsername());
        userDO.setPasswordHash(PasswordUtils.hashPassword(reqVO.getPassword()));
        userDO.setRole(UserRoleEnum.USER);
        userDO.setProfile(profile);
        userDO.setIsActive(Boolean.TRUE);
        return userDO;
    }

    private UserLoginRspVO storeSessionAndBuildRsp(UserDO userDO, HttpSession session) {
        session.setAttribute(SessionConstants.CURRENT_USER, UserAssembler.toSessionDTO(userDO));
        return UserAssembler.toLoginRspVO(userDO);
    }

    private String assertUsernameUnique(String username) {
        if (StringUtils.isBlank(username)) {
            return ResponseCodeEnum.PARAM_NOT_VALID.getErrorMsg();
        }
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username).isNull(UserDO::getDeletedAt);
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return ResponseCodeEnum.USERNAME_ALREADY_EXISTS.getErrorMsg();
        }
        return null;
    }

    private String assertEmailAvailable(String email) {
        UserDO user = loadUserByEmailInternal(email);
        if (user != null && (user.getId() != null)) {
            return ResponseCodeEnum.EMAIL_ALREADY_REGISTERED.getErrorMsg();
        }
        return null;
    }

    private UserDO loadUserByEmail(String email) {
        return loadUserByEmailInternal(email);
    }

    private UserDO loadUserByEmailInternal(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        QueryWrapper<UserDO> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");
        wrapper.apply("profile ->> 'email' = {0}", email);
        return userMapper.selectOne(wrapper);
    }
}
