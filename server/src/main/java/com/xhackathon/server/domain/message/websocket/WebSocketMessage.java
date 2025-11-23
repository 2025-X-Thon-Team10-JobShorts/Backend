package com.xhackathon.server.domain.message.websocket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebSocketMessage {
    private MessageType type;
    private String currentUserPid;
    private Long threadId;
    private Object data;

    @JsonCreator
    public WebSocketMessage(
            @JsonProperty("type") MessageType type,
            @JsonProperty("currentUserPid") String currentUserPid,
            @JsonProperty("threadId") Long threadId,
            @JsonProperty("data") Object data) {
        this.type = type;
        this.currentUserPid = currentUserPid;
        this.threadId = threadId;
        this.data = data;
    }

    public enum MessageType {
        INIT,
        SUBSCRIBE,
        MESSAGE_CREATED,
        MESSAGE_READ
    }
}