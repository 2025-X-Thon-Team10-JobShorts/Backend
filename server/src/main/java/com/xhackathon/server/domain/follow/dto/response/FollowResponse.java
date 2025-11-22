package com.xhackathon.server.domain.follow.dto.response;

import lombok.Getter;

@Getter
public class FollowResponse {

    private Long followId;

    private boolean following; //true:팔로우, false:언팔로우

    private String message;

    public FollowResponse(Long followId, boolean following, String message) {
        this.followId = followId;
        this.following = following;
        this.message = message;
    }

}
