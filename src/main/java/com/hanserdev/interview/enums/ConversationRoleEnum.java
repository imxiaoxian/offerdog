package com.hanserdev.interview.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum ConversationRoleEnum {

    INTERVIEWER("interviewer"),
    CANDIDATE("candidate");

    @EnumValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static ConversationRoleEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return INTERVIEWER;
        }
        for (ConversationRoleEnum item : values()) {
            if (item.dbValue.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return INTERVIEWER;
    }
}
