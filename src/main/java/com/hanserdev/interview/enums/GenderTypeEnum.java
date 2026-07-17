package com.hanserdev.interview.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum GenderTypeEnum {

    MALE("male"),
    FEMALE("female"),
    OTHER("other");

    @EnumValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static GenderTypeEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return OTHER;
        }
        for (GenderTypeEnum genderTypeEnum : values()) {
            if (genderTypeEnum.dbValue.equalsIgnoreCase(value)) {
                return genderTypeEnum;
            }
        }
        return OTHER;
    }
}
