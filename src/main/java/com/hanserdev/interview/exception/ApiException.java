package com.hanserdev.interview.exception;

import com.hanserdev.interview.enums.ResponseCodeEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends RuntimeException {
    // 异常码
    private String errorCode;
    // 异常信息
    private String errorMsg;

    public ApiException(BaseExceptionInterface baseExceptionInterface) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMsg = baseExceptionInterface.getErrorMsg();
    }

    /**
     * 使用枚举中的 errorCode，覆盖展示给前端的 message（用于可诊断的业务错误）。
     */
    public ApiException(ResponseCodeEnum code, String customMessage) {
        this.errorCode = code.getErrorCode();
        this.errorMsg = (customMessage != null && !customMessage.isBlank())
                ? customMessage
                : code.getErrorMsg();
    }
}
