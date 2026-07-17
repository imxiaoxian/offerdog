package com.hanserdev.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.response.FileUploadResp;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.model.vo.user.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 * <p>
 * 提供用户相关的业务逻辑接口定义
 */
public interface UserService {

    /**
     * 创建新用户
     *
     * @param reqVO 用户创建请求参数
     * @return 新创建用户的ID
     */
    Long createUser(UserCreateReqVO reqVO);

    /**
     * 更新用户基本信息
     *
     * @param id    用户ID
     * @param reqVO 用户更新请求参数
     */
    void updateUser(Long id, UserUpdateReqVO reqVO);

    /**
     * 上传并解析用户简历文件
     *
     * @param id   用户ID
     * @param file 简历文件
     * @return 文件上传响应（包含解析后的简历信息）
     */
    UserResumeRspVO uploadAndParseResume(Long id, MultipartFile file);

    /**
     * 更新用户简历信息
     *
     * @param id    用户ID
     * @param reqVO 用户简历更新请求参数
     */
    void updateResume(Long id, UserResumeUpdateReqVO reqVO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 获取用户详细信息
     *
     * @param id 用户ID
     * @return 用户详细信息
     */
    UserDetailRspVO getUserDetail(Long id);

    /**
     * 分页查询用户列表
     *
     * @param reqVO 用户查询请求参数
     * @return 用户摘要信息分页结果
     */
    IPage<UserSummaryRspVO> pageUsers(UserQueryReqVO reqVO);

    /**
     * 获取用户简历信息
     *
     * @param id 用户ID
     * @return 用户简历信息
     */
    UserResumeDTO getUserResume(Long id);

    /**
     * 获取用户资料信息
     *
     * @param id 用户ID
     * @return 用户资料信息
     */
    UserProfileDTO getUserProfile(Long id);

    /**
     * 上传用户头像
     *
     * @param id   用户ID
     * @param file 头像文件
     * @return 文件上传响应
     */
    FileUploadResp uploadAvatar(Long id, MultipartFile file);
}
