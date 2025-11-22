package com.xhackathon.server.domain.shortform.dto.response;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormStatus;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class ShortFormResponse {

    private Long id;
    private String ownerPid;
    private String title;
    private String description;
    private String videoKey;
    private String thumbnailKey;
    private Integer durationSec;
    private ShortFormStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ShortFormResponse from(ShortForm entity) {
        ShortFormResponse res = new ShortFormResponse();
        res.id = entity.getId();
        res.ownerPid = entity.getOwnerPid();
        res.title = entity.getTitle();
        res.description = entity.getDescription();
        res.videoKey = entity.getVideoKey();
        res.thumbnailKey = entity.getThumbnailKey();
        res.durationSec = entity.getDurationSec();
        res.status = entity.getStatus();
        res.createdAt = entity.getCreatedAt();
        res.updatedAt = entity.getUpdatedAt();
        return res;
    }
}