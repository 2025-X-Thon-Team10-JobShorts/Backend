package com.xhackathon.server.domain.mypage.service;

import com.xhackathon.server.domain.mypage.dto.request.MypageUpdateRequest;
import com.xhackathon.server.domain.mypage.dto.response.*;
import com.xhackathon.server.domain.mypage.entity.ApplicantBookmark;
import com.xhackathon.server.domain.mypage.repository.ApplicantBookmarkRepository;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final ShortFormRepository shortFormRepository;
    private final ApplicantBookmarkRepository applicantBookmarkRepository;

    @Transactional(readOnly = true)
    public MypageResponse getMyPage(String pid) {
        User user = userRepository.findById(pid)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        List<String> videoKeys = shortFormRepository.findByOwnerPid(pid)
                .stream()
                .map(ShortForm::getVideoKey)
                .toList();

        return MypageResponse.from(user, videoKeys);
    }

    @Transactional
    public MypageResponse updateMyPage(MypageUpdateRequest req) {
        User user = userRepository.findById(req.getPid())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        user.updateProfile(
                req.getBio(),
                req.getPortfolioLink(),
                req.getPortfolioFileUrl()
        );

        List<String> videoKeys = shortFormRepository.findByOwnerPid(req.getPid())
                .stream()
                .map(ShortForm::getVideoKey)
                .toList();

        return MypageResponse.from(user, videoKeys);
    }

    @Transactional(readOnly = true)
    public MyPageInfoResponse getMyPageInfo(String pid) {
        User user = userRepository.findById(pid)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        
        return new MyPageInfoResponse(user);
    }

    @Transactional(readOnly = true)
    public List<ShortForm> getMyPosts(String pid) {
        return shortFormRepository.findByOwnerPid(pid);
    }

    @Transactional(readOnly = true)
    public BookmarkedApplicantListResponse getBookmarkedApplicants(String companyPid, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<ApplicantBookmark> bookmarkPage = applicantBookmarkRepository
                .findByCompanyPidOrderByCreatedAtDesc(companyPid, pageable);

        List<BookmarkedApplicantResponse> applicants = bookmarkPage.getContent().stream()
                .map(bookmark -> {
                    User applicant = userRepository.findById(bookmark.getApplicantPid())
                            .orElseThrow(() -> new IllegalArgumentException("지원자를 찾을 수 없습니다."));
                    
                    List<String> skills = shortFormRepository.findByOwnerPid(applicant.getPid())
                            .stream()
                            .filter(shortForm -> shortForm.getTags() != null)
                            .flatMap(shortForm -> shortForm.getTags().stream())
                            .distinct()
                            .toList();

                    return new BookmarkedApplicantResponse(
                            applicant.getPid(),
                            applicant.getDisplayName(),
                            applicant.getBio() != null ? applicant.getBio() : "개발자",
                            applicant.getProfileImageUrl(),
                            skills,
                            "경력정보 없음",
                            "위치정보 없음",
                            true,
                            bookmark.getCreatedAt()
                    );
                })
                .toList();

        long total = applicantBookmarkRepository.countByCompanyPid(companyPid);
        boolean hasNext = bookmarkPage.hasNext();

        return new BookmarkedApplicantListResponse(applicants, total, page, limit, hasNext);
    }

    @Transactional
    public BookmarkToggleResponse toggleApplicantBookmark(String companyPid, String applicantPid) {
        if (!userRepository.existsById(applicantPid)) {
            throw new IllegalArgumentException("지원자를 찾을 수 없습니다.");
        }

        boolean isBookmarked = applicantBookmarkRepository.existsByCompanyPidAndApplicantPid(companyPid, applicantPid);
        
        if (isBookmarked) {
            applicantBookmarkRepository.deleteByCompanyPidAndApplicantPid(companyPid, applicantPid);
            return new BookmarkToggleResponse(applicantPid, false, "북마크에서 제거되었습니다");
        } else {
            applicantBookmarkRepository.save(new ApplicantBookmark(companyPid, applicantPid));
            return new BookmarkToggleResponse(applicantPid, true, "북마크에 추가되었습니다");
        }
    }
}
