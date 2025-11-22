package com.xhackathon.server.domain.companypage.service;

import com.xhackathon.server.domain.bookmark.entity.Bookmark;
import com.xhackathon.server.domain.bookmark.repository.BookmarkRepository;
import com.xhackathon.server.domain.companypage.dto.BookmarkedShortFormInfo;
import com.xhackathon.server.domain.companypage.dto.response.CompanyPageResponse;
import com.xhackathon.server.domain.follow.repository.FollowRepository;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.shortform.service.AwsS3Service;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyPageService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ShortFormRepository shortFormRepository;
    private final AwsS3Service awsS3Service;

    @Transactional(readOnly = true)
    public CompanyPageResponse getCompanyPage(String companyPid) {

        User company = userRepository.findById(companyPid)
                .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다."));

        // 1) 팔로워 수 조회
        int followerCnt = followRepository.countByFolloweePid(companyPid);

        // 2) 북마크한 숏폼 조회
        List<Bookmark> bookmarks = bookmarkRepository.findByCompanyPid(companyPid);
        int bookmarkCnt = bookmarks.size();

        // 3) 숏폼 상세 데이터 만들기
        List<BookmarkedShortFormInfo> shortForms = bookmarks.stream()
                .map(b -> {
                    ShortForm sf = shortFormRepository.findById(b.getShortFormId())
                            .orElseThrow(() -> new IllegalArgumentException("숏폼을 찾을 수 없음"));

                    User owner = userRepository.findById(sf.getOwnerPid())
                            .orElseThrow(() -> new IllegalArgumentException("소유자 없음"));

                    String summary = awsS3Service.getSummary(
                            sf.getId(),
                            sf.getVideoKey()
                    );

                    return BookmarkedShortFormInfo.builder()
                            .shortFormId(sf.getId())
                            .summary(summary)
                            .ownerName(owner.getDisplayName())
                            .ownerProfileImageUrl(owner.getProfileImageUrl())
                            .build();
                })
                .toList();

        return CompanyPageResponse.builder()
                .loginId(company.getLoginId())
                .companyName(company.getDisplayName())
                .description(company.getPortfolioLink())
                .followerCount(followerCnt)
                .bookmarkCount(bookmarkCnt)
                .bookmarkedShortForms(shortForms)
                .build();
    }
}
