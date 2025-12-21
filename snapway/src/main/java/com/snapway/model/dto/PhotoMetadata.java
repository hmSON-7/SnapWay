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
    private LocalDateTime takenAt; // 촬영 일시
    private Double latitude;       // 위도
    private Double longitude;      // 경도
    
    /**
     * 위치 정보가 유효한지 확인합니다.
     * @return 위도, 경도 정보가 모두 있으면 true
     */
    public boolean hasLocation() {
        return latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0;
    }
}