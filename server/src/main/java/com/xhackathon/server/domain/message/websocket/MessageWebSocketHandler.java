package com.xhackathon.server.domain.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
            
            switch (wsMessage.getType()) {
                case INIT:
                    handleInit(session, wsMessage);
                    break;
                case SUBSCRIBE:
                    handleSubscribe(session, wsMessage);
                    break;
                default:
                    log.warn("Unknown message type: {}", wsMessage.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    private void handleInit(WebSocketSession session, WebSocketMessage message) {
        String userPid = message.getCurrentUserPid();
        if (userPid != null) {
            sessionManager.addSession(userPid, session);
        }
    }

    private void handleSubscribe(WebSocketSession session, WebSocketMessage message) {
        String userPid = sessionManager.findUserBySession(session);
        if (userPid != null && message.getThreadId() != null) {
            sessionManager.subscribeToThread(userPid, message.getThreadId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userPid = sessionManager.findUserBySession(session);
        if (userPid != null) {
            sessionManager.removeSession(userPid);
        }
        log.info("WebSocket connection closed: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
    }
}