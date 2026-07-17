package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserdev.interview.common.response.FileUploadResp;
import com.hanserdev.interview.constants.AIPromptConstants;
import com.hanserdev.interview.domain.dataobject.UserDO;
import com.hanserdev.interview.domain.mapper.UserMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.UserAssembler;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.model.vo.user.*;
import com.hanserdev.interview.service.ResumeUploadService;
import com.hanserdev.interview.service.UserService;
import com.hanserdev.interview.service.InterviewStructuredExtractService;
import com.hanserdev.interview.utils.FileUtils;
import com.hanserdev.interview.utils.PasswordUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ResumeUploadService resumeUploadService;

    @Resource
    private InterviewStructuredExtractService interviewStructuredExtractService;

    @Resource
    private FileUtils fileUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReqVO reqVO) {
        checkUsernameUnique(reqVO.getUsername(), null);
        String passwordHash = PasswordUtils.hashPassword(reqVO.getPassword());
        UserDO userDO = UserAssembler.toUserDO(reqVO, passwordHash);
        if (userMapper.insert(userDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        return userDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserUpdateReqVO reqVO) {
        UserDO userDO = loadActiveUser(id);
        if (StringUtils.isNotBlank(reqVO.getUsername())
                && !Strings.CS.equals(reqVO.getUsername(), userDO.getUsername())) {
            checkUsernameUnique(reqVO.getUsername(), id);
        }
        String passwordHash = null;
        if (StringUtils.isNotBlank(reqVO.getPassword())) {
            passwordHash = PasswordUtils.hashPassword(reqVO.getPassword());
        }
        UserAssembler.merge(reqVO, userDO, passwordHash);
        if (userMapper.updateById(userDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResumeRspVO uploadAndParseResume(Long id, MultipartFile file) {
        Objects.requireNonNull(file, "文件不能为空");

        // 1. 验证用户存在
        loadActiveUser(id);

        // 2. 解析简历文件，提取结构化信息
        log.info("开始解析用户 {} 的简历文件", id);

        // 3. 上传文件到 MinIO
        log.info("开始上传简历文件到MinIO");
        String fileUrl;
        try {
            fileUrl = resumeUploadService.uploadResume(file);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("简历上传存储失败 userId={}", id, e);
            throw new ApiException(ResponseCodeEnum.RESUME_STORAGE_FAILED);
        }

        // 4. 本地转文本后由 DeepSeek 抽取结构化简历
        List<UserResumeDTO> userResumeDTOS;
        try {
            byte[] fileContent = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            userResumeDTOS = interviewStructuredExtractService.extractFromFileContent(
                    fileContent,
                    originalFilename != null ? originalFilename : "resume.pdf",
                    UserResumeDTO.class,
                    AIPromptConstants.RESUME_PARSE_PROMPT_TEMPLATE
            );
        } catch (InterviewStructuredExtractService.ExtractionException e) {
            log.warn("简历结构化抽取失败 userId={}: {}", id, e.getMessage());
            throw new ApiException(ResponseCodeEnum.RESUME_PARAS_FAILED,
                    e.getMessage() != null ? e.getMessage() : ResponseCodeEnum.RESUME_PARAS_FAILED.getErrorMsg());
        } catch (IOException e) {
            log.warn("简历读文件失败 userId={}", id, e);
            throw new ApiException(ResponseCodeEnum.RESUME_PARAS_FAILED, "简历文件读取失败: " + e.getMessage());
        }
        if (CollectionUtils.isEmpty(userResumeDTOS)) {
            throw new ApiException(ResponseCodeEnum.RESUME_PARAS_FAILED);
        }
        UserResumeDTO userResumeDTO = userResumeDTOS.getFirst();
        UserResumeRspVO rspVO = new UserResumeRspVO();
        BeanUtils.copyProperties(userResumeDTO, rspVO);
        return rspVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResume(Long id, UserResumeUpdateReqVO reqVO) {
        Objects.requireNonNull(reqVO, "请求参数不能为空");
        UserDO userDO = loadActiveUser(id);
        userDO.setResume(reqVO.getResume());
        if (userMapper.updateById(userDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        UserDO userDO = loadActiveUser(id);
        userDO.setDeletedAt(LocalDateTime.now());
        if (userMapper.updateById(userDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailRspVO getUserDetail(Long id) {
        UserDO userDO = loadActiveUser(id);
        return UserAssembler.toDetailRspVO(userDO);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<UserSummaryRspVO> pageUsers(UserQueryReqVO reqVO) {
        long pageNo = reqVO.getPageNo() == null ? 1L : reqVO.getPageNo();
        long pageSize = reqVO.getPageSize() == null ? DEFAULT_PAGE_SIZE : Math.min(reqVO.getPageSize(), MAX_PAGE_SIZE);
        Page<UserDO> page = new Page<>(pageNo, pageSize);
        Page<UserDO> userPage = userMapper.selectPage(page, buildQueryWrapper(reqVO));
        Page<UserSummaryRspVO> rspPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        rspPage.setRecords(userPage.getRecords().stream()
                .map(UserAssembler::toSummaryRspVO)
                .collect(Collectors.toList()));
        return rspPage;
    }

    @Override
    public UserResumeDTO getUserResume(Long id) {
        UserDO userDO = loadActiveUser(id);
        return userDO.getResume();
    }

    @Override
    public UserProfileDTO getUserProfile(Long id) {
        UserDO userDO = loadActiveUser(id);
        return userDO.getProfile();
    }

    private UserDO loadActiveUser(Long id) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getId, id).isNull(UserDO::getDeletedAt);
        UserDO userDO = userMapper.selectOne(wrapper);
        if (userDO == null) {
            throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        return userDO;
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username).isNull(UserDO::getDeletedAt);
        if (excludeId != null) {
            wrapper.ne(UserDO::getId, excludeId);
        }
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(ResponseCodeEnum.USERNAME_ALREADY_EXISTS);
        }
    }

    @Override
    public FileUploadResp uploadAvatar(Long id, MultipartFile file) {
        String ext = resolveAvatarExtension(file);
        String fileName = "avatar" + id + ext;
        fileUtils.uploadFile(file, fileName);
        // 相对站点根路径，经 Vite 代理或同源访问；隧道/手机无需再打通 MinIO 端口
        String url = "/public/avatars/" + fileName;
        return FileUploadResp.builder()
                .fileUrl(url)
                .build();
    }

    private static String resolveAvatarExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String e = original.substring(original.lastIndexOf('.')).toLowerCase();
            if (e.matches("\\.(png|jpe?g|gif|webp|bmp|heic|heif)")) {
                return e;
            }
        }
        String ct = file.getContentType();
        if (ct == null) {
            return ".jpg";
        }
        if (ct.contains("png")) {
            return ".png";
        }
        if (ct.contains("gif")) {
            return ".gif";
        }
        if (ct.contains("webp")) {
            return ".webp";
        }
        if (ct.contains("heic") || ct.contains("heif")) {
            return ".heic";
        }
        return ".jpg";
    }

    private QueryWrapper<UserDO> buildQueryWrapper(UserQueryReqVO reqVO) {
        QueryWrapper<UserDO> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");
        if (StringUtils.isNotBlank(reqVO.getUsername())) {
            wrapper.like("username", reqVO.getUsername());
        }
        if (reqVO.getRole() != null) {
            wrapper.apply("role = {0}::user_role", reqVO.getRole().getDbValue());
        }
        if (reqVO.getIsActive() != null) {
            wrapper.eq("is_active", reqVO.getIsActive());
        }
        if (StringUtils.isNotBlank(reqVO.getEmail())) {
            wrapper.apply("profile ->> 'email' = {0}", reqVO.getEmail());
        }
        if (StringUtils.isNotBlank(reqVO.getSkill())) {
            wrapper.apply("COALESCE(resume -> 'skills', '[]'::jsonb) ? {0}", reqVO.getSkill());
        }
        wrapper.orderByDesc("created_at");
        return wrapper;
    }
}
