package com.xhackathon.server.domain.mypage.dto.response;

import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.entity.UserRole;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class MypageResponse {

    private String loginId;
    private String displayName;
    private UserRole role;
    private String profileImageUrl;
    private String bio;
    private String portfolioLink;
    private String portfolioFileUrl;
    private List<String> videoKeyList;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MypageResponse from(User user, List<String> videoKeys) {
        MypageResponse res = new MypageResponse();
        res.loginId = user.getLoginId();
        res.displayName = user.getDisplayName();
        res.role = user.getRole();
        res.profileImageUrl = user.getProfileImageUrl();
        res.bio = user.getBio();
        res.portfolioLink = user.getPortfolioLink();
        res.portfolioFileUrl = user.getPortfolioFileUrl();
        res.videoKeyList = videoKeys;
        res.createdAt = user.getCreatedAt();
        res.updatedAt = user.getUpdatedAt();
        return res;
    }
}
