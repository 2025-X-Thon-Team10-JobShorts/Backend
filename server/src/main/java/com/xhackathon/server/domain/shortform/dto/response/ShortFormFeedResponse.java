package com.xhackathon.server.domain.shortform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ShortFormFeedResponse {
    private List<ShortFormReelsResponse> data;
    private String nextPageParam;
    private Boolean hasNextPage;
    
    public static ShortFormFeedResponse of(List<ShortFormReelsResponse> data, String nextPageParam, Boolean hasNextPage) {
        return new ShortFormFeedResponse(data, nextPageParam, hasNextPage);
    }
}