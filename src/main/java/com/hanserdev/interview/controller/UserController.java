package com.hanserdev.interview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.FileUploadResp;
import com.hanserdev.interview.common.response.PageResponse;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.model.vo.user.*;
import com.hanserdev.interview.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ApiOperationLog(description = "创建用户")
    public Response<Long> createUser(@Valid @RequestBody UserCreateReqVO reqVO) {
        return Response.success(userService.createUser(reqVO));
    }

    @PutMapping("/{id}")
    @ApiOperationLog(description = "更新用户信息")
    public Response<Void> updateUser(@PathVariable("id") Long id,
                                     @Valid @RequestBody UserUpdateReqVO reqVO) {
        userService.updateUser(id, reqVO);
        return Response.success();
    }

    /**
     * 上传并解析用户简历文件
     */
    @PostMapping("/{id}/resume/upload")
    @ApiOperationLog(description = "上传并解析用户简历文件")
    public Response<UserResumeRspVO> uploadAndParseResume(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        return Response.success(userService.uploadAndParseResume(id, file));
    }

    @PatchMapping("/{id}/resume")
    @ApiOperationLog(description = "更新用户简历")
    public Response<Void> updateResume(@PathVariable("id") Long id,
                                       @Valid @RequestBody UserResumeUpdateReqVO reqVO) {
        userService.updateResume(id, reqVO);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperationLog(description = "软删除用户")
    public Response<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return Response.success();
    }

    @GetMapping("/{id}")
    @ApiOperationLog(description = "查询用户详情")
    public Response<UserDetailRspVO> getUserDetail(@PathVariable("id") Long id) {
        return Response.success(userService.getUserDetail(id));
    }

    @GetMapping
    @ApiOperationLog(description = "分页查询用户列表")
    public PageResponse<UserSummaryRspVO> pageUsers(@Valid UserQueryReqVO reqVO) {
        IPage<UserSummaryRspVO> page = userService.pageUsers(reqVO);
        return PageResponse.success(page.getRecords(), page.getCurrent(), page.getTotal(), page.getSize());
    }

    @GetMapping("/{id}/resume")
    @ApiOperationLog(description = "查询用户简历")
    public Response<UserResumeDTO> getUserResume(@PathVariable("id") Long id) {
        return Response.success(userService.getUserResume(id));
    }

    @GetMapping("/{id}/profile")
    @ApiOperationLog(description = "查询用户资料")
    public Response<UserProfileDTO> getUserProfile(@PathVariable("id") Long id) {
        return Response.success(userService.getUserProfile(id));
    }

    // 上传用户头像文件
    @PostMapping("/{id}/avatar")
    @ApiOperationLog(description = "上传用户头像文件")
    public Response<FileUploadResp> uploadAvatar(@PathVariable("id") Long id,
                                                @RequestParam("file") MultipartFile  file){
        return Response.success(userService.uploadAvatar(id, file));
    }
}
