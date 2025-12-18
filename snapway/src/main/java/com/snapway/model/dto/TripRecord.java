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
public class TripRecord {
    private int recordId;
    private int tripId; // FK
    private Double latitude; // 위도
    private Double longitude; // 경도
    private String placeName; // 장소명 (Kakao API로 변환 예정)
    private String aiContent; // AI 일기 (나중에 사용)
    private LocalDateTime visitedDate; // 촬영 시간
}