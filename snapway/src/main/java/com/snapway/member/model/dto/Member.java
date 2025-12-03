package com.snapway.member.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    
    private int id;
    private String email;
    private String password;
    private String username;
    
    // DB의 ENUM 타입과 매핑
    private Role role; 
    
    // DB: DATETIME -> Java: LocalDateTime
    private LocalDateTime createdAt;
    
    private String profileImg;
    
    private Gender gender;
    
    // DB: DATE -> Java: LocalDate
    private LocalDate birthday;
    
    private TravelStyle style; 
}