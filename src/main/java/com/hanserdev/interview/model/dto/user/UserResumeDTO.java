package com.hanserdev.interview.model.dto.user;

import com.hanserdev.interview.enums.GenderTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 用户简历数据传输对象
 */
@Data
public class UserResumeDTO {

    /**
     * 姓名，最大长度50个字符
     */
    @Size(max = 50)
    private String name;

    /**
     * 出生日期
     */
    private LocalDate birth;

    /**
     * 性别
     */
    private GenderTypeEnum gender;

    /**
     * 所在地区信息
     */
    @Valid
    private Location location;

    /**
     * 教育背景信息列表
     */
    @Valid
    private List<Education> education;

    /**
     * 联系方式信息
     */
    @Valid
    private Contact contact;

    /**
     * 工作经历信息
     */
    @Valid
    private Work work;

    /**
     * 技能列表，每个技能最大长度50个字符
     */
    private List<@Size(max = 50) String> skills;

    /**
     * 求职意向，最大长度100个字符
     */
    @Size(max = 100)
    private String jobIntention;

    /**
     * 自我评价，最大长度1000个字符
     */
    @Size(max = 1000)
    private String selfEvaluation;

    /**
     * 简历文件URL，最大长度255个字符
     */
    @Size(max = 255)
    private String resumeFileUrl;

    /**
     * 地区信息
     */
    @Data
    public static class Location {
        /**
         * 省份，最大长度50个字符
         */
        @Size(max = 50)
        private String province;

        /**
         * 城市，最大长度50个字符
         */
        @Size(max = 50)
        private String city;
    }

    /**
     * 教育背景信息
     */
    @Data
    public static class Education {
        /**
         * 学历等级，最大长度30个字符
         */
        @Size(max = 30)
        private String level;

        /**
         * 学校名称，最大长度100个字符
         */
        @Size(max = 100)
        private String school;

        /**
         * 专业，最大长度50个字符
         */
        @Size(max = 50)
        private String major;

        /**
         * 毕业时间
         */
        private YearMonth graduationDate;
    }

    /**
     * 联系方式信息
     */
    @Data
    public static class Contact {
        /**
         * 手机号码，最大长度20个字符
         */
        @Size(max = 20)
        private String phone;

        /**
         * 邮箱地址，最大长度100个字符
         */
        @Email
        @Size(max = 100)
        private String email;

        /**
         * 个人主页，最大长度255个字符
         */
        @Size(max = 255)
        private String homepage;
    }

    /**
     * 工作经历信息
     */
    @Data
    public static class Work {
        /**
         * 首次就业时间
         */
        private YearMonth firstEmployment;

        /**
         * 工作经历列表
         */
        @Valid
        private List<WorkExperience> experience;

        /**
         * 项目经历列表
         */
        @Valid
        private List<ProjectExperience> projects;
    }

    /**
     * 工作经历详情
     */
    @Data
    public static class WorkExperience {
        /**
         * 公司名称，最大长度100个字符
         */
        @Size(max = 100)
        private String company;

        /**
         * 职位，最大长度100个字符
         */
        @Size(max = 100)
        private String title;

        /**
         * 开始时间
         */
        private YearMonth startDate;

        /**
         * 结束时间
         */
        private YearMonth endDate;

        /**
         * 工作描述，最大长度1000个字符
         */
        @Size(max = 1000)
        private String description;
    }

    /**
     * 项目经历详情
     */
    @Data
    public static class ProjectExperience {
        /**
         * 项目名称，最大长度100个字符
         */
        @Size(max = 100)
        private String name;

        /**
         * 项目角色，最大长度100个字符
         */
        @Size(max = 100)
        private String role;

        /**
         * 项目描述，最大长度1000个字符
         */
        @Size(max = 1000)
        private String description;

        /**
         * 项目亮点列表，每个亮点最大长度100个字符
         */
        private List<@Size(max = 100) String> highlights;
    }
}
