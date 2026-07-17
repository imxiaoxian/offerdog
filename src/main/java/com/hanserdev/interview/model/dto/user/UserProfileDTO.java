package com.hanserdev.interview.model.dto.user;

import com.hanserdev.interview.enums.GenderTypeEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料传输对象
 * 包含用户的基本信息，如联系方式和个人资料
 */
@Data
public class UserProfileDTO {

    /**
     * 用户邮箱地址
     * 必须是有效的邮箱格式，不能为空，最长100个字符
     */
    @Email
    @NotBlank
    @Size(max = 100)
    private String email;

    /**
     * 用户手机号码
     * 不能为空，最长20个字符
     */
    @NotBlank
    @Size(max = 20)
    private String phone;

    /**
     * 用户真实姓名
     * 不能为空，最长50个字符
     */
    @NotBlank
    @Size(max = 50)
    private String realName;

    /**
     * 用户头像URL
     * 可选字段，最长255个字符
     */
    @Size(max = 255)
    private String avatarUrl;

    /**
     * 用户性别
     * 使用GenderTypeEnum枚举类型，默认值为OTHER(其他)
     */
    private GenderTypeEnum gender = GenderTypeEnum.OTHER;
}
