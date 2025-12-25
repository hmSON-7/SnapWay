package com.snapway.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;

    // 키에 해당하는 값 조회
    public String getData(String key) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(key);
    }
    
    // 데이터 저장
    public void setData(String key, String value) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value);
    }
    
    // 만료 시간을 지정한 뒤 데이터 저장(초 단위)
    public void setDataExpire(String key, String value, long duration) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        Duration expireDuration = Duration.ofSeconds(duration);
        valueOperations.set(key, value, expireDuration);
    }

    // 데이터 삭제 (반환값: 삭제 성공 여부)
    public boolean deleteData(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
    
    // 키 존재 여부 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}