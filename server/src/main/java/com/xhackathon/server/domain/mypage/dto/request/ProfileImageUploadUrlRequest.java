package com.xhackathon.server.domain.mypage.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileImageUploadUrlRequest {
    private String pid;
    private String fileName;
    private String mimeType;
}