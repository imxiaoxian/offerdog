package com.hanserdev.interview.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionDifficultyEnum {

    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    @EnumValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static QuestionDifficultyEnum fromValue(String value) {
        for (QuestionDifficultyEnum item : values()) {
            if (item.dbValue.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return MEDIUM;
    }
}
