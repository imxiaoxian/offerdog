package com.hanserdev.interview.service;

import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.vo.user.*;
import jakarta.servlet.http.HttpSession;

public interface AuthService {

    /**
     * 发送注册验证码
     * @param reqVO 注册验证码发送请求参数
     * @param session HTTP会话
     * @return 响应结果
     */
    Response<?> sendRegisterCode(UserEmailRegisterCodeSendReqVO reqVO, HttpSession session);

    /**
     * 邮箱注册
     * @param reqVO 邮箱注册请求参数
     * @param session HTTP会话
     * @return 用户登录响应信息
     */
    UserLoginRspVO registerByEmail(UserEmailRegisterReqVO reqVO, HttpSession session);

    /**
     * 发送登录验证码
     * @param reqVO 登录验证码发送请求参数
     * @param session HTTP会话
     * @return 响应结果
     */
    Response<?> sendLoginCode(UserEmailLoginCodeSendReqVO reqVO, HttpSession session);

    /**
     * 邮箱验证码登录
     * @param reqVO 邮箱验证码登录请求参数
     * @param session HTTP会话
     * @return 用户登录响应信息
     */
    UserLoginRspVO loginByEmailCode(UserEmailLoginByCodeReqVO reqVO, HttpSession session);

    /**
     * 邮箱密码登录
     * @param reqVO 邮箱密码登录请求参数
     * @param session HTTP会话
     * @return 响应结果
     */
    Response<?> loginByEmailPassword(UserEmailLoginByPasswordReqVO reqVO, HttpSession session);
}
