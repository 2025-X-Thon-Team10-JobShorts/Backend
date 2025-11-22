package com.xhackathon.server.domain.shortform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnerInfo {
    private Long id;
    private String displayName;
    private String profileImageUrl;
    private Boolean isFollowed;
    
    public static OwnerInfo of(Long id, String displayName, String profileImageUrl, Boolean isFollowed) {
        return new OwnerInfo(id, displayName, profileImageUrl, isFollowed);
    }
}