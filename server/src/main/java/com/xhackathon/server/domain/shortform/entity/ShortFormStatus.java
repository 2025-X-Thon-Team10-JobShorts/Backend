package com.xhackathon.server.domain.shortform.entity;

public enum ShortFormStatus {
    UPLOADING,
    READY,
    PROCESSING_STT,
    READY_WITH_AI,
    FAILED
}
