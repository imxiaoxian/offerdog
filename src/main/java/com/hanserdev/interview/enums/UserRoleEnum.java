package com.hanserdev.interview.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    USER("user"),
    ADMIN("admin"),
    VIP("vip");

    @EnumValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static UserRoleEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return USER;
        }
        for (UserRoleEnum userRoleEnum : values()) {
            if (userRoleEnum.dbValue.equalsIgnoreCase(value)) {
                return userRoleEnum;
            }
        }
        return USER;
    }
}
