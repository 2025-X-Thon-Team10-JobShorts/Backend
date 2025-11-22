package com.xhackathon.server.domain.mypage.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MypageUpdateRequest {

    private String pid;

    private String bio;
    private String portfolioLink;
    private String portfolioFileUrl;

    public MypageUpdateRequest() {}
}
