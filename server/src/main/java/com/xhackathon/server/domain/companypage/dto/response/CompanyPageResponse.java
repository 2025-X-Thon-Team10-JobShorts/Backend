package com.xhackathon.server.domain.companypage.dto.response;

import com.xhackathon.server.domain.companypage.dto.BookmarkedShortFormInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CompanyPageResponse {

    private String loginId;
    private String companyName;
    private String description;   // portfolioLink 활용
    private int followerCount;
    private int bookmarkCount;

    private List<BookmarkedShortFormInfo> bookmarkedShortForms;
}
