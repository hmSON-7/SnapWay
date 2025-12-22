package com.snapway.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Trip;
import com.snapway.model.service.TripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;

    /**
     * AI 여행 기록 자동 생성 API
     * POST /trip/auto-create
     * @param title 여행 제목
     * @param files 사진 파일 리스트 (form-data: files)
     * @return 생성된 여행 정보
     */
    @PostMapping("/auto-create")
    public ResponseEntity<?> createAutoTrip(
            @RequestParam("title") String title,
            @RequestParam("files") List<MultipartFile> files
            // @AuthenticationPrincipal CustomUserDetails userDetails // 추후 인증 적용 시
    ) {
        // 임시 사용자 ID (로그인 구현 후 userDetails.getMemberId()로 변경)
        int memberId = 1; 

        log.info("AI 여행 기록 생성 요청 - 제목: {}, 파일 수: {}", title, files.size());

        try {
            Trip createdTrip = tripService.createAutoTrip(memberId, title, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTrip);
        } catch (Exception e) {
            log.error("여행 기록 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("여행 기록 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}