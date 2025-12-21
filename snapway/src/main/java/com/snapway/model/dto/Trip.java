package com.snapway.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {
    private int tripId;
    private String title;
    private String tags;
    private int memberId; // 작성자 ID (DB컬럼: id)
    private LocalDateTime uploadedAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private String visibility; // PUBLIC or PRIVATE
    
    private List<TripRecord> records;
}