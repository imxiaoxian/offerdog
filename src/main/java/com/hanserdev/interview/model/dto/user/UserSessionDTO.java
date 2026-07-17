package com.hanserdev.interview.model.dto.user;

import com.hanserdev.interview.enums.UserRoleEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserSessionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String email;
    private UserRoleEnum role;
}
