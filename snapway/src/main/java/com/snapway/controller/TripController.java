package com.snapway.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.PhotoMetadata;
import com.snapway.model.service.AiService;
import com.snapway.model.service.TripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final AiService aiService;

    /**
     * 사진 분석 (POST /api/trip/analyze)
     * 업로드된 사진들의 메타데이터(위치, 시간)를 추출하여 반환
     * (아직 DB에 저장하지 않음)
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzePhotos(@RequestParam("photos") List<MultipartFile> photos) {
        Map<String, Object> resultMap = new HashMap<>();
        
        if (photos == null || photos.isEmpty()) {
            resultMap.put("message", "files_empty");
            return new ResponseEntity<>(resultMap, HttpStatus.BAD_REQUEST);
        }

        try {
            // Service 호출
            List<PhotoMetadata> analyzedData = tripService.extractMetadata(photos);
            
            resultMap.put("data", analyzedData);
            resultMap.put("message", "success");
            resultMap.put("count", analyzedData.size());
            
            return new ResponseEntity<>(resultMap, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("사진 분석 중 오류 발생", e);
            resultMap.put("message", "error");
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/ai-test")
    public String testAi(@RequestParam(defaultValue = "Hello Gemini") String prompt) {
    	log.info("AI Test Request: {}", prompt);
    	return aiService.generateContent(prompt);
    }
}