package com.xhackathon.server.domain.bookmark.service;

import com.xhackathon.server.domain.bookmark.dto.request.BookmarkRequest;
import com.xhackathon.server.domain.bookmark.dto.response.BookmarkResponse;
import com.xhackathon.server.domain.bookmark.entity.Bookmark;
import com.xhackathon.server.domain.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public BookmarkResponse addBookmark(BookmarkRequest req) {

        if (bookmarkRepository.existsByCompanyPidAndShortFormId(req.getCompanyPid(), req.getShortFormId())) {
            return new BookmarkResponse("이미 북마크한 숏폼입니다.");
        }

        Bookmark bookmark = new Bookmark(req.getCompanyPid(), req.getShortFormId());
        bookmarkRepository.save(bookmark);

        return new BookmarkResponse("북마크 추가 완료");
    }

    @Transactional
    public BookmarkResponse removeBookmark(BookmarkRequest req) {

        Bookmark bookmark = bookmarkRepository.findByCompanyPidAndShortFormId(
                req.getCompanyPid(),
                req.getShortFormId()
        ).orElseThrow(() -> new IllegalArgumentException("북마크가 존재하지 않습니다."));

        bookmarkRepository.delete(bookmark);

        return new BookmarkResponse("북마크 삭제 완료");
    }
}