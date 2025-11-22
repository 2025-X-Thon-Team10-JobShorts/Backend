package com.xhackathon.server.domain.shortform.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShortFormCreateRequest {

    private String ownerPid;
    private String title;
    private String description;
    private String videoKey;    // Step1 응답에서 받은 videoKey
    private Integer durationSec;
    private List<String> tags;
}
