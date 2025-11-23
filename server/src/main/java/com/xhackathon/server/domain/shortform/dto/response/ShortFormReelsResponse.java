package com.xhackathon.server.domain.shortform.dto.response;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormAiStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ShortFormReelsResponse {
    private Long id;
    private OwnerInfo owner;
    private String videoUrl;
    private String thumbnailUrl;
    private String title;
    private String description;
    private Integer durationSec;
    private String summary;
    private List<String> tags;
    private OffsetDateTime createdAt;
    private ShortFormAiStatus aiStatus;
    
    public static ShortFormReelsResponse of(ShortForm shortForm, OwnerInfo owner, String videoUrl, String thumbnailUrl, String summary, ShortFormAiStatus aiStatus) {
        return new ShortFormReelsResponse(
                shortForm.getId(),
                owner,
                videoUrl,
                thumbnailUrl,
                shortForm.getTitle(),
                shortForm.getDescription(),
                shortForm.getDurationSec(),
                summary,
                shortForm.getTags(),
                shortForm.getCreatedAt(),
                aiStatus
        );
    }
}