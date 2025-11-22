package com.xhackathon.server.domain.shortform.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortFormUploadUrlRequest {

    private String fileName; // intro.mp4
    private String mimeType; // video/mp4
    private Long size;       // 파일 사이즈 (바이트) - 일단 참고용
}
