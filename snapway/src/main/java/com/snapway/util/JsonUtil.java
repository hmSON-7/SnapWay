package com.snapway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// 이 유틸은 json문자열을 받아서 자바 객체로 변환하는 유틸입니다.
// 사용하기 위해서는 jackson이 필요합니다. 정상 작동하지 않는다면 pom.xml에 명시적으로 의존성을 추가해주세요.
public final class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtil() {}

    /**
     * JSON 문자열을 지정한 타입의 자바 객체로 변환합니다.
     *
     * @param json:  JSON 문자열
     * @param clazz:  변환할 대상 클래스 타입
     * @param <T>   제네릭 타입
     * @return 변환된 객체, 실패 시 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            System.out.println("JSON 파싱 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
