package com.tetz.kb6_back.controller;

import com.tetz.kb6_back.dto.UserDto;
import com.tetz.kb6_back.service.UserSQLService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
public class SQLController {
    private final UserSQLService userSQLService;

    @PostMapping
    public ResponseEntity<Void> saveUser(@RequestBody UserDto user) {
        userSQLService.saveUser(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        UserDto user = userSQLService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userSQLService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping
    public ResponseEntity<Void> updateUser(@RequestBody UserDto user) {
        userSQLService.updateUser(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userSQLService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}
