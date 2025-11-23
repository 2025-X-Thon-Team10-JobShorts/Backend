package com.xhackathon.server.domain.companypage.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkedShortFormInfo {
    private Long shortFormId;

    private String summary;

    private String ownerName;
    private String ownerProfileImageUrl;
}
