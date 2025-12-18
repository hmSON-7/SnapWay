package com.snapway.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoMetadata {
    private String originalFileName; // 원본 파일명
    private Double latitude; // 위도
    private Double longitude; // 경도
    private LocalDateTime dateTaken; // 촬영 시간
    
    // 분석 성공 여부 (GPS 정보가 없는 사진일 수도 있음)
    private boolean hasLocation;
}