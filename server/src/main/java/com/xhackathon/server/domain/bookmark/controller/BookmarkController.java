package com.xhackathon.server.domain.bookmark.controller;

import com.xhackathon.server.domain.bookmark.dto.request.BookmarkRequest;
import com.xhackathon.server.domain.bookmark.dto.response.BookmarkResponse;
import com.xhackathon.server.domain.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmark")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/add")
    public ResponseEntity<BookmarkResponse> addBookmark(@RequestBody BookmarkRequest request) {
        return ResponseEntity.ok(bookmarkService.addBookmark(request));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<BookmarkResponse> removeBookmark(@RequestBody BookmarkRequest request) {
        return ResponseEntity.ok(bookmarkService.removeBookmark(request));
    }
}
