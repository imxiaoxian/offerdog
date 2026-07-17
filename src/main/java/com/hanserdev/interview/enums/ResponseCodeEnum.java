package com.hanserdev.interview.enums;

import com.hanserdev.interview.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("INTERVIEW-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("INTERVIEW-10001", "参数错误"),
    UNAUTHORIZED("INTERVIEW-10002", "用户未登录"),

    // ----------- 业务异常状态码 -----------
    TOKEN_EXPIRED("INTERVIEW-20001", "验证码已过期"),
    USERNAME_ALREADY_EXISTS("INTERVIEW-20002", "用户名已存在"),
    USER_NOT_FOUND("INTERVIEW-20003", "用户不存在"),
    EMAIL_ALREADY_REGISTERED("INTERVIEW-20004", "邮箱已注册"),
    EMAIL_NOT_FOUND("INTERVIEW-20005", "邮箱未注册"),
    EMAIL_CODE_INVALID("INTERVIEW-20006", "邮箱验证码错误或已失效"),
    EMAIL_SEND_FAILED("INTERVIEW-20007", "邮箱验证码发送失败"),
    PASSWORD_INCORRECT("INTERVIEW-20008", "邮箱密码错误"),
    CATEGORY_NAME_DUPLICATE("INTERVIEW-20009", "同一层级名称已存在"),
    CATEGORY_NOT_FOUND("INTERVIEW-20010", "分类不存在"),
    CATEGORY_LEVEL_INVALID("INTERVIEW-20011", "分类层级不合法"),
    QUESTION_BANK_NOT_FOUND("INTERVIEW-20012", "题库不存在"),
    QUESTION_NOT_FOUND("INTERVIEW-20013", "题目不存在"),
    USER_ROLE_INVALID("INTERVIEW-20014", "权限不足"),
    RESUME_PARAS_FAILED("INTERVIEW-20015", "简历解析失败"),
    QUESTION_ALREADY_FAVORITE("INTERVIEW-20016", "题目已收藏"),
    RESUME_FILE_INVALID("INTERVIEW-20017", "简历格式不支持或文件过大"),
    RESUME_STORAGE_FAILED("INTERVIEW-20018", "简历文件存储失败，请检查 MinIO 是否已启动且配置正确"),

    // ----------- 面试相关异常状态码 -----------
    PARAM_ERROR("INTERVIEW-30001", "参数错误"),
    INTERVIEW_TEMPLATE_NOT_FOUND("INTERVIEW-30002", "面试模板不存在"),
    INTERVIEW_SESSION_NOT_FOUND("INTERVIEW-30003", "面试会话不存在"),
    INTERVIEW_REPORT_NOT_FOUND("INTERVIEW-30004", "面试报告不存在"),
    RESUME_NOT_FOUND("INTERVIEW-30005", "简历不存在"),
    INTERVIEW_PLAN_TEMPLATE_NOT_FOUND("INTERVIEW-30006", "面试计划模板不存在"),
    /** DeepSeek / OpenAI 兼容接口调用失败（密钥、网络、限流等） */
    LLM_CALL_FAILED("INTERVIEW-30007", "AI 对话服务暂时不可用，请稍后重试或检查 DEEPSEEK_API_KEY");

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMsg;

}
