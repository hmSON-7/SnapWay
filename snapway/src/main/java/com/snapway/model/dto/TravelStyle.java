package com.snapway.model.dto;

import lombok.Getter;

@Getter
public enum TravelStyle {
    NATURE("자연 힐링"),
    CITY("도시 탐험"),
    FOOD("맛집 탐방"),
    ACTIVITY("액티비티"),
    CULTURE("문화 예술"),
    PHOTO("인생샷"),
    HEALING("휴양 휴식");

    private final String description;

    TravelStyle(String description) {
        this.description = description;
    }
}