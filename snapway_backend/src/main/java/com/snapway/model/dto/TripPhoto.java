package com.snapway.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPhoto {
    private int photoCode;
    private int recordId; // FK
    private String filePath; // 서버 저장 경로 (/uploads/...)
    private String photoName; // 원본 파일명
    private String caption;
}