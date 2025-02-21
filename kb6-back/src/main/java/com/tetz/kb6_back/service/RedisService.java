package com.tetz.kb6_back.service;

import com.tetz.kb6_back.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "user:";

    public void saveUser(UserDto user) {
        String key = KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, user);
    }

    public UserDto getUser(String userId) {
        String key = KEY_PREFIX + userId;
        return (UserDto) redisTemplate.opsForValue().get(key);
    }

    public void deleteUser(String userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    public void saveUserWithExpiration(UserDto user, long timeout, TimeUnit timeUnit) {
        String key = KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, user, timeout, timeUnit);
    }
}
