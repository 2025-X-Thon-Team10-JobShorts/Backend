package com.xhackathon.server.domain.shortform.dto.response;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormStatus;
import com.xhackathon.server.domain.shortform.entity.VisibilityType;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class ShortFormDetailResponse {

    private Long id;
    private String ownerPid;

    private String title;
    private String description;

    private String videoKey;
    private String thumbnailKey;

    private Integer durationSec;
    private List<String> tags;
    private ShortFormStatus status;

    private String summary;
    private String transcript;
    private String videoUrl;
    private String thumbnailUrl;

    private VisibilityType visibility;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;


    public static ShortFormDetailResponse of(ShortForm sf, String summary) {
        ShortFormDetailResponse res = new ShortFormDetailResponse();
        res.id = sf.getId();
        res.ownerPid = sf.getOwnerPid();
        res.title = sf.getTitle();
        res.description = sf.getDescription();
        res.videoKey = sf.getVideoKey();
        res.thumbnailKey = sf.getThumbnailKey();
        res.durationSec = sf.getDurationSec();
        res.tags = sf.getTags();
        res.status = sf.getStatus();
        res.summary = summary;
        res.visibility = sf.getVisibility();

        res.createdAt = sf.getCreatedAt();
        res.updatedAt = sf.getUpdatedAt();
        return res;
    }

    public static ShortFormDetailResponse of(ShortForm sf, String summary, String transcript, String videoUrl, String thumbnailUrl) {
        ShortFormDetailResponse res = new ShortFormDetailResponse();
        res.id = sf.getId();
        res.ownerPid = sf.getOwnerPid();
        res.title = sf.getTitle();
        res.description = sf.getDescription();
        res.videoKey = sf.getVideoKey();
        res.thumbnailKey = sf.getThumbnailKey();
        res.durationSec = sf.getDurationSec();
        res.tags = sf.getTags();
        res.status = sf.getStatus();
        res.summary = summary;
        res.transcript = transcript;
        res.videoUrl = videoUrl;
        res.thumbnailUrl = thumbnailUrl;
        res.visibility = sf.getVisibility();

        res.createdAt = sf.getCreatedAt();
        res.updatedAt = sf.getUpdatedAt();
        return res;
    }
}
