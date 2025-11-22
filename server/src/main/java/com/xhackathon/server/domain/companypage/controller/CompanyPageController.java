package com.xhackathon.server.domain.companypage.controller;

import com.xhackathon.server.domain.companypage.dto.request.CompanyPageRequest;
import com.xhackathon.server.domain.companypage.dto.response.CompanyPageResponse;
import com.xhackathon.server.domain.companypage.service.CompanyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/company")
public class CompanyPageController {

    private final CompanyPageService companyPageService;

    @PostMapping("/page")
    public ResponseEntity<CompanyPageResponse> getCompanyPage(@RequestBody CompanyPageRequest req) {
        return ResponseEntity.ok(companyPageService.getCompanyPage(req.getPid()));
    }
}
