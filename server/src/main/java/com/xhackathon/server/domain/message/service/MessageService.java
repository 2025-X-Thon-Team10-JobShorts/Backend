package com.xhackathon.server.domain.message.service;

import com.xhackathon.server.domain.message.dto.request.CreateThreadRequest;
import com.xhackathon.server.domain.message.dto.request.SendMessageRequest;
import com.xhackathon.server.domain.message.dto.response.MessageResponse;
import com.xhackathon.server.domain.message.dto.response.ThreadMessagesResponse;
import com.xhackathon.server.domain.message.dto.response.ThreadResponse;
import com.xhackathon.server.domain.message.entity.Message;
import com.xhackathon.server.domain.message.entity.Thread;
import com.xhackathon.server.domain.message.repository.MessageRepository;
import com.xhackathon.server.domain.message.repository.ThreadRepository;
import com.xhackathon.server.domain.message.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final ThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    public List<ThreadResponse> getUserThreads(String userPid) {
        List<Thread> threads = threadRepository.findThreadsByUserPid(userPid);
        return threads.stream()
                .map(thread -> {
                    long unreadCount = messageRepository.countUnreadMessagesInThread(thread.getId(), userPid);
                    return ThreadResponse.from(thread, unreadCount);
                })
                .collect(Collectors.toList());
    }

    public ThreadResponse createThread(CreateThreadRequest request) {
        Thread existingThread = threadRepository
                .findThreadBetweenUsers(request.getParticipant1Pid(), request.getParticipant2Pid())
                .orElse(null);
        
        if (existingThread != null) {
            return ThreadResponse.from(existingThread);
        }

        Thread newThread = new Thread(request.getParticipant1Pid(), request.getParticipant2Pid());
        Thread savedThread = threadRepository.save(newThread);
        
        return ThreadResponse.from(savedThread);
    }

    public ThreadMessagesResponse getThreadMessages(Long threadId, String userPid) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));

        if (!thread.isParticipant(userPid)) {
            throw new IllegalArgumentException("User is not a participant of this thread");
        }

        List<Message> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
        List<MessageResponse> messageResponses = messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());

        markMessagesAsRead(threadId, userPid);

        ThreadResponse threadResponse = ThreadResponse.from(thread);
        return new ThreadMessagesResponse(threadResponse, messageResponses);
    }

    public MessageResponse sendMessage(Long threadId, SendMessageRequest request) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));

        if (!thread.isParticipant(request.getSenderPid())) {
            throw new IllegalArgumentException("User is not a participant of this thread");
        }

        Message message = new Message(thread, request.getSenderPid(), request.getContent());
        Message savedMessage = messageRepository.save(message);

        thread.updateLastActivity();
        threadRepository.save(thread);

        MessageResponse messageResponse = MessageResponse.from(savedMessage);
        
        // WebSocket 실시간 알림 전송
        webSocketNotificationService.notifyMessageCreated(thread, messageResponse);

        return messageResponse;
    }

    public void markMessagesAsRead(Long threadId, String userPid) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));
                
        List<Message> unreadMessages = messageRepository.findUnreadMessagesInThread(threadId, userPid);
        unreadMessages.forEach(Message::markAsRead);
        messageRepository.saveAll(unreadMessages);
        
        // WebSocket 읽음 알림 전송
        unreadMessages.forEach(message -> {
            webSocketNotificationService.notifyMessageRead(thread, userPid, message.getId());
        });
    }
}