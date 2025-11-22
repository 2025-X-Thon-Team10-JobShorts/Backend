package com.xhackathon.server.domain.shortform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShortFormUploadUrlResponse {

    private String uploadUrl; // presigned URL
    private String videoKey;  // videos/{uuid}.mp4
}
