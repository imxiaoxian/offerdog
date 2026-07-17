package com.hanserdev.interview.controller;

import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.vo.user.UserEmailLoginByCodeReqVO;
import com.hanserdev.interview.model.vo.user.UserEmailLoginByPasswordReqVO;
import com.hanserdev.interview.model.vo.user.UserEmailLoginCodeSendReqVO;
import com.hanserdev.interview.model.vo.user.UserEmailRegisterCodeSendReqVO;
import com.hanserdev.interview.model.vo.user.UserEmailRegisterReqVO;
import com.hanserdev.interview.model.vo.user.UserLoginRspVO;
import com.hanserdev.interview.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/email/register/code")
    @ApiOperationLog(description = "发送邮箱注册验证码")
    public Response<?> sendRegisterCode(@Valid @RequestBody UserEmailRegisterCodeSendReqVO reqVO,
                                           HttpSession session) {
        return authService.sendRegisterCode(reqVO, session);
    }

    @PostMapping("/email/register")
    @ApiOperationLog(description = "邮箱验证码注册")
    public Response<UserLoginRspVO> register(@Valid @RequestBody UserEmailRegisterReqVO reqVO,
                                             HttpSession session) {
        return Response.success(authService.registerByEmail(reqVO, session));
    }

    @PostMapping("/email/login/code/send")
    @ApiOperationLog(description = "发送邮箱登录验证码")
    public Response<?> sendLoginCode(@Valid @RequestBody UserEmailLoginCodeSendReqVO reqVO,
                                        HttpSession session) {
        return authService.sendLoginCode(reqVO, session);
    }

    @PostMapping("/email/login/code")
    @ApiOperationLog(description = "邮箱验证码登录")
    public Response<UserLoginRspVO> loginByCode(@Valid @RequestBody UserEmailLoginByCodeReqVO reqVO,
                                                HttpSession session) {
        return Response.success(authService.loginByEmailCode(reqVO, session));
    }

    @PostMapping("/email/login/password")
    @ApiOperationLog(description = "邮箱密码登录")
    public Response<?> loginByPassword(@Valid @RequestBody UserEmailLoginByPasswordReqVO reqVO,
                                                   HttpSession session) {
        return authService.loginByEmailPassword(reqVO, session);
    }
}
