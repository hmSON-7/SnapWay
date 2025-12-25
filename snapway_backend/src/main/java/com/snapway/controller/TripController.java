package com.snapway.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Member;
import com.snapway.model.dto.Trip;
import com.snapway.model.service.TripService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;

    /**
     * 1. AI 여행 기록 자동 생성
     */
    @PostMapping("/auto-create")
    public ResponseEntity<?> createAutoTrip(
            @RequestParam("title") String title,
            @RequestParam("files") List<MultipartFile> files,
            HttpSession session
    ) {
    	
    	Member loginUser = (Member) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        log.info("AI 여행 기록 생성 요청 - 사용자: {}, 제목: {}, 파일 수: {}", loginUser.getId(), title, files.size());

        try {
            Trip createdTrip = tripService.createAutoTrip(loginUser.getId(), title, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTrip);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("여행 기록 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("여행 기록 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 2. 내 여행 기록 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<?> getMyTripList(HttpSession session) {
        Member loginUser = (Member) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            List<Trip> trips = tripService.getMyTripList(loginUser.getId());
            return ResponseEntity.ok(trips);
        } catch (Exception e) {
            log.error("여행 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("목록 조회 실패");
        }
    }

    /**
     * 3. 여행 기록 상세 조회
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<?> getTripDetail(@PathVariable("tripId") int tripId) {
        try {
            Trip trip = tripService.getTripDetail(tripId);
            return ResponseEntity.ok(trip);
        } catch (Exception e) {
            log.error("여행 상세 조회 실패: {}", tripId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "해당 여행 기록을 찾을 수 없습니다."));
        }
    }
}