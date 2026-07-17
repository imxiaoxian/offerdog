package com.hanserdev.interview.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionSourceEnum {

    OFFICIAL("official"),
    USER("user");

    @EnumValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static QuestionSourceEnum fromValue(String value) {
        for (QuestionSourceEnum item : values()) {
            if (item.dbValue.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return USER;
    }
}
