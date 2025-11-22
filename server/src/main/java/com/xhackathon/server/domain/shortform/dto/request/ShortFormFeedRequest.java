package com.xhackathon.server.domain.shortform.dto.request;

import lombok.Data;

@Data
public class ShortFormFeedRequest {
    private String pageParam;
    private int size = 10;
    private String currentUserPid;
}