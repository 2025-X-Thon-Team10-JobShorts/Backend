package com.xhackathon.server.domain.message.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {
    
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> userSubscriptions = new ConcurrentHashMap<>();

    public void addSession(String userPid, WebSocketSession session) {
        userSessions.put(userPid, session);
        userSubscriptions.putIfAbsent(userPid, ConcurrentHashMap.newKeySet());
        log.info("User {} connected with session {}", userPid, session.getId());
    }

    public void removeSession(String userPid) {
        WebSocketSession session = userSessions.remove(userPid);
        userSubscriptions.remove(userPid);
        if (session != null) {
            log.info("User {} disconnected", userPid);
        }
    }

    public WebSocketSession getSession(String userPid) {
        return userSessions.get(userPid);
    }

    public void subscribeToThread(String userPid, Long threadId) {
        Set<Long> subscriptions = userSubscriptions.get(userPid);
        if (subscriptions != null) {
            subscriptions.add(threadId);
            log.info("User {} subscribed to thread {}", userPid, threadId);
        }
    }

    public void unsubscribeFromThread(String userPid, Long threadId) {
        Set<Long> subscriptions = userSubscriptions.get(userPid);
        if (subscriptions != null) {
            subscriptions.remove(threadId);
            log.info("User {} unsubscribed from thread {}", userPid, threadId);
        }
    }

    public boolean isSubscribedToThread(String userPid, Long threadId) {
        Set<Long> subscriptions = userSubscriptions.get(userPid);
        return subscriptions != null && subscriptions.contains(threadId);
    }

    public Set<String> getConnectedUsers() {
        return userSessions.keySet();
    }

    public String findUserBySession(WebSocketSession session) {
        return userSessions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}