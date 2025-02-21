package com.tetz.kb6_back.controller;

import com.tetz.kb6_back.dto.UserDto;
import com.tetz.kb6_back.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class RedisController {
    private final RedisService redisService;

    @PostMapping
    public ResponseEntity<Void> saveUser(@RequestBody UserDto user) {
        redisService.saveUserWithExpiration(user, 1, TimeUnit.HOURS);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        UserDto user = redisService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        redisService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}