package com.hanserdev.interview.model.convert;

import com.hanserdev.interview.domain.dataobject.UserDO;
import com.hanserdev.interview.enums.UserRoleEnum;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.user.UserCreateReqVO;
import com.hanserdev.interview.model.vo.user.UserDetailRspVO;
import com.hanserdev.interview.model.vo.user.UserLoginRspVO;
import com.hanserdev.interview.model.vo.user.UserSummaryRspVO;
import com.hanserdev.interview.model.vo.user.UserUpdateReqVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * 用户对象转换器
 * 提供用户相关对象之间的转换方法
 */
public final class UserAssembler {

    /**
     * 私有构造函数，防止实例化
     */
    private UserAssembler() {
    }

    /**
     * 将创建用户请求VO转换为用户数据对象
     *
     * @param reqVO         创建用户请求VO
     * @param passwordHash  密码哈希值
     * @return 用户数据对象
     */
    public static UserDO toUserDO(UserCreateReqVO reqVO, String passwordHash) {
        UserDO userDO = new UserDO();
        userDO.setUsername(reqVO.getUsername());
        userDO.setPasswordHash(passwordHash);
        userDO.setRole(Optional.ofNullable(reqVO.getRole()).orElse(UserRoleEnum.USER));
        userDO.setProfile(reqVO.getProfile());
        userDO.setResume(reqVO.getResume());
        userDO.setIsActive(reqVO.getIsActive() == null || reqVO.getIsActive());
        return userDO;
    }

    /**
     * 将更新用户请求VO合并到用户数据对象中
     *
     * @param reqVO        更新用户请求VO
     * @param userDO       用户数据对象
     * @param passwordHash 密码哈希值
     */
    public static void merge(UserUpdateReqVO reqVO, UserDO userDO, String passwordHash) {
        if (StringUtils.isNotBlank(reqVO.getUsername())) {
            userDO.setUsername(reqVO.getUsername());
        }
        if (passwordHash != null) {
            userDO.setPasswordHash(passwordHash);
        }
        if (reqVO.getRole() != null) {
            userDO.setRole(reqVO.getRole());
        }
        if (reqVO.getProfile() != null) {
            userDO.setProfile(reqVO.getProfile());
        }
        if (reqVO.getResume() != null) {
            userDO.setResume(reqVO.getResume());
        }
        if (reqVO.getIsActive() != null) {
            userDO.setIsActive(reqVO.getIsActive());
        }
    }

    /**
     * 将用户数据对象转换为详细信息响应VO
     *
     * @param userDO 用户数据对象
     * @return 用户详细信息响应VO
     */
    public static UserDetailRspVO toDetailRspVO(UserDO userDO) {
        UserDetailRspVO rspVO = new UserDetailRspVO();
        rspVO.setId(userDO.getId());
        rspVO.setUsername(userDO.getUsername());
        rspVO.setRole(userDO.getRole());
        rspVO.setProfile(userDO.getProfile());
        rspVO.setResume(userDO.getResume());
        rspVO.setIsActive(userDO.getIsActive());
        rspVO.setCreatedAt(userDO.getCreatedAt());
        rspVO.setUpdatedAt(userDO.getUpdatedAt());
        return rspVO;
    }

    /**
     * 将用户数据对象转换为摘要信息响应VO
     *
     * @param userDO 用户数据对象
     * @return 用户摘要信息响应VO
     */
    public static UserSummaryRspVO toSummaryRspVO(UserDO userDO) {
        UserSummaryRspVO rspVO = new UserSummaryRspVO();
        rspVO.setId(userDO.getId());
        rspVO.setUsername(userDO.getUsername());
        rspVO.setRole(userDO.getRole());
        rspVO.setProfile(userDO.getProfile());
        rspVO.setIsActive(userDO.getIsActive());
        rspVO.setCreatedAt(userDO.getCreatedAt());
        rspVO.setUpdatedAt(userDO.getUpdatedAt());
        return rspVO;
    }

    public static UserLoginRspVO toLoginRspVO(UserDO userDO) {
        UserLoginRspVO rspVO = new UserLoginRspVO();
        rspVO.setId(userDO.getId());
        rspVO.setUsername(userDO.getUsername());
        rspVO.setRole(userDO.getRole());
        rspVO.setIsActive(userDO.getIsActive());
        rspVO.setCreatedAt(userDO.getCreatedAt());
        rspVO.setUpdatedAt(userDO.getUpdatedAt());
        rspVO.setEmail(extractEmail(userDO.getProfile()));
        return rspVO;
    }

    public static UserSessionDTO toSessionDTO(UserDO userDO) {
        UserSessionDTO sessionDTO = new UserSessionDTO();
        sessionDTO.setUserId(userDO.getId());
        sessionDTO.setUsername(userDO.getUsername());
        sessionDTO.setRole(userDO.getRole());
        sessionDTO.setEmail(extractEmail(userDO.getProfile()));
        return sessionDTO;
    }

    private static String extractEmail(UserProfileDTO profile) {
        return profile == null ? null : profile.getEmail();
    }
}
