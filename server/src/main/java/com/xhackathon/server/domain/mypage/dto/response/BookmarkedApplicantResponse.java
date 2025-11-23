package com.xhackathon.server.domain.mypage.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class BookmarkedApplicantResponse {
    private final String id;
    private final String name;
    private final String position;
    private final String avatar;
    private final List<String> skills;
    private final String experience;
    private final String location;
    private final boolean isBookmarked;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final OffsetDateTime bookmarkedAt;

    public BookmarkedApplicantResponse(String id, String name, String position, String avatar, 
                                     List<String> skills, String experience, String location, 
                                     boolean isBookmarked, OffsetDateTime bookmarkedAt) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.avatar = avatar;
        this.skills = skills;
        this.experience = experience;
        this.location = location;
        this.isBookmarked = isBookmarked;
        this.bookmarkedAt = bookmarkedAt;
    }
}