package com.xhackathon.server.domain.mypage.dto.response;

import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.entity.UserRole;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class MyPageInfoResponse {
    private final boolean success = true;
    private final MyPageData data;

    public MyPageInfoResponse(User user) {
        this.data = new MyPageData(user);
    }

    @Getter
    public static class MyPageData {
        private final String pid;
        private final String loginId;
        private final String displayName;
        private final UserRole role;
        private final String profileImageUrl;
        private final String bio;
        private final String portfolioLink;
        private final String portfolioFileUrl;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        public MyPageData(User user) {
            this.pid = user.getPid();
            this.loginId = user.getLoginId();
            this.displayName = user.getDisplayName();
            this.role = user.getRole();
            this.profileImageUrl = user.getProfileImageUrl();
            this.bio = user.getBio();
            this.portfolioLink = user.getPortfolioLink();
            this.portfolioFileUrl = user.getPortfolioFileUrl();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
        }
    }
}