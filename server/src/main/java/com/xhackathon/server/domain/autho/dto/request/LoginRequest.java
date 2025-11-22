package com.xhackathon.server.domain.autho.dto.request;

import com.xhackathon.server.domain.user.entity.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String loginId;
    private String password;
    private UserRole role;
    private String displayName;

    public LoginRequest() {
    }

}
