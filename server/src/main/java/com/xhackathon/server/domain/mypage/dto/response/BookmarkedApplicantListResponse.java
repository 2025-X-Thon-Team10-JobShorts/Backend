package com.xhackathon.server.domain.mypage.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class BookmarkedApplicantListResponse {
    private final boolean success = true;
    private final List<BookmarkedApplicantResponse> data;
    private final long total;
    private final PaginationInfo pagination;

    public BookmarkedApplicantListResponse(List<BookmarkedApplicantResponse> data, long total, 
                                         int page, int limit, boolean hasNext) {
        this.data = data;
        this.total = total;
        this.pagination = new PaginationInfo(page, limit, hasNext);
    }

    @Getter
    public static class PaginationInfo {
        private final int page;
        private final int limit;
        private final boolean hasNext;

        public PaginationInfo(int page, int limit, boolean hasNext) {
            this.page = page;
            this.limit = limit;
            this.hasNext = hasNext;
        }
    }
}