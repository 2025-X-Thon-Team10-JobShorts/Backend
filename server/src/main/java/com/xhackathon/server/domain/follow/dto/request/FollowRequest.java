package com.xhackathon.server.domain.follow.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowRequest {

    private String followerPid;

    private String followeePid;

    public FollowRequest() {}

}
