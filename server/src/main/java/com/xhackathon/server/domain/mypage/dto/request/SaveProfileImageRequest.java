package com.xhackathon.server.domain.mypage.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveProfileImageRequest {
    private String pid;
    private String imageKey;
}
