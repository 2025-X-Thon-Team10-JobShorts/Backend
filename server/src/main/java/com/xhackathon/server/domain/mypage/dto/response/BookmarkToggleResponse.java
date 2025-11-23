package com.xhackathon.server.domain.mypage.dto.response;

import lombok.Getter;

@Getter
public class BookmarkToggleResponse {
    private final boolean success = true;
    private final BookmarkToggleData data;

    public BookmarkToggleResponse(String applicantId, boolean isBookmarked, String message) {
        this.data = new BookmarkToggleData(applicantId, isBookmarked, message);
    }

    @Getter
    public static class BookmarkToggleData {
        private final String applicantId;
        private final boolean isBookmarked;
        private final String message;

        public BookmarkToggleData(String applicantId, boolean isBookmarked, String message) {
            this.applicantId = applicantId;
            this.isBookmarked = isBookmarked;
            this.message = message;
        }
    }
}