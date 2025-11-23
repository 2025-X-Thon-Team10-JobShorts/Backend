package com.xhackathon.server.domain.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhackathon.server.domain.message.dto.response.MessageResponse;
import com.xhackathon.server.domain.message.entity.Thread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public void notifyMessageCreated(Thread thread, MessageResponse message) {
        try {
            String otherParticipant = thread.getOtherParticipant(message.getSenderPid());
            WebSocketSession session = sessionManager.getSession(otherParticipant);
            
            if (session != null && session.isOpen() && 
                sessionManager.isSubscribedToThread(otherParticipant, thread.getId())) {
                
                WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.MESSAGE_CREATED,
                    null,
                    thread.getId(),
                    message
                );
                
                String jsonMessage = objectMapper.writeValueAsString(wsMessage);
                session.sendMessage(new TextMessage(jsonMessage));
                
                log.info("Sent message notification to user {}", otherParticipant);
            }
        } catch (Exception e) {
            log.error("Error sending WebSocket notification", e);
        }
    }

    public void notifyMessageRead(Thread thread, String readerPid, Long messageId) {
        try {
            String otherParticipant = thread.getOtherParticipant(readerPid);
            WebSocketSession session = sessionManager.getSession(otherParticipant);
            
            if (session != null && session.isOpen() && 
                sessionManager.isSubscribedToThread(otherParticipant, thread.getId())) {
                
                MessageReadNotification readData = new MessageReadNotification();
                readData.setMessageId(messageId);
                readData.setReaderPid(readerPid);
                readData.setReadAt(java.time.OffsetDateTime.now());
                
                WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.MESSAGE_READ,
                    null,
                    thread.getId(),
                    readData
                );
                
                String jsonMessage = objectMapper.writeValueAsString(wsMessage);
                session.sendMessage(new TextMessage(jsonMessage));
                
                log.info("Sent read notification to user {}", otherParticipant);
            }
        } catch (Exception e) {
            log.error("Error sending read notification", e);
        }
    }

    private static class MessageReadNotification {
        private Long messageId;
        private String readerPid;
        private java.time.OffsetDateTime readAt;

        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        
        public String getReaderPid() { return readerPid; }
        public void setReaderPid(String readerPid) { this.readerPid = readerPid; }
        
        public java.time.OffsetDateTime getReadAt() { return readAt; }
        public void setReadAt(java.time.OffsetDateTime readAt) { this.readAt = readAt; }
    }
}