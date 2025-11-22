package com.xhackathon.server.domain.mypage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.URL;

@Getter
@AllArgsConstructor
public class ProfileImageUploadUrlResponse {
    private URL uploadUrl;
    private String imageKey;
}