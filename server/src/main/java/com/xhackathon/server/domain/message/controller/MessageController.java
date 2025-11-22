package com.xhackathon.server.domain.message.controller;

import com.xhackathon.server.domain.message.dto.request.CreateThreadRequest;
import com.xhackathon.server.domain.message.dto.request.SendMessageRequest;
import com.xhackathon.server.domain.message.dto.request.ThreadListRequest;
import com.xhackathon.server.domain.message.dto.response.MessageResponse;
import com.xhackathon.server.domain.message.dto.response.ThreadMessagesResponse;
import com.xhackathon.server.domain.message.dto.response.ThreadResponse;
import com.xhackathon.server.domain.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/threads")
    public ResponseEntity<List<ThreadResponse>> getThreads(@RequestBody ThreadListRequest request) {
        List<ThreadResponse> threads = messageService.getUserThreads(request.getUserPid());
        return ResponseEntity.ok(threads);
    }

    @PostMapping("/threads")
    public ResponseEntity<ThreadResponse> createThread(@RequestBody CreateThreadRequest request) {
        ThreadResponse thread = messageService.createThread(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(thread);
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ThreadMessagesResponse> getThreadMessages(
            @PathVariable Long threadId,
            @RequestParam String userPid) {
        ThreadMessagesResponse response = messageService.getThreadMessages(threadId, userPid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long threadId,
            @RequestBody SendMessageRequest request) {
        MessageResponse message = messageService.sendMessage(threadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}