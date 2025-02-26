package com.tetz.kb6_back.controller;

import com.tetz.kb6_back.service.PerformanceTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/performance-test")
@RequiredArgsConstructor
public class PerformanceTestController {
    private final PerformanceTestService performanceTestService;

    @GetMapping("/run/{dataCount}")
    public ResponseEntity<String> runTest(@PathVariable int dataCount) {
        performanceTestService.runComparisonTest(dataCount);
        return ResponseEntity.ok("성능 테스트가 완료되었습니다. 로그를 확인하세요.");
    }

    @GetMapping("/compare/{dataCount}")
    public ResponseEntity<Map<String, Object>> comparePerformance(@PathVariable int dataCount) {
        Map<String, Object> results = new HashMap<>();

        // 테스트 데이터 생성
        var users = performanceTestService.generateTestUsers(dataCount);

        // 저장 성능 테스트
        long redisSaveTime = performanceTestService.testRedisSave(users);
        long mysqlSaveTime = performanceTestService.testMySQLSave(users);

        // 조회 성능 테스트
        long redisReadTime = performanceTestService.testRedisRead(users);
        long mysqlReadTime = performanceTestService.testMySQLRead(users);

        // 결과 저장
        results.put("dataCount", dataCount);

        Map<String, Object> saveResults = new HashMap<>();
        saveResults.put("redis", redisSaveTime);
        saveResults.put("mysql", mysqlSaveTime);
        saveResults.put("ratio", (double)mysqlSaveTime / redisSaveTime);
        results.put("save", saveResults);

        Map<String, Object> readResults = new HashMap<>();
        readResults.put("redis", redisReadTime);
        readResults.put("mysql", mysqlReadTime);
        readResults.put("ratio", (double)mysqlReadTime / redisReadTime);
        results.put("read", readResults);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/redis-read/{dataCount}")
    public ResponseEntity<Long> testRedisRead(@PathVariable int dataCount) {
        long time = performanceTestService.testRedisBulkRead(dataCount);
        return ResponseEntity.ok(time);
    }

    @GetMapping("/mysql-read/{dataCount}")
    public ResponseEntity<Long> testMySQLRead(@PathVariable int dataCount) {
        long time = performanceTestService.testMySQLBulkRead(dataCount);
        return ResponseEntity.ok(time);
    }

    @GetMapping("/mysql-findall/{dataCount}")
    public ResponseEntity<Long> testMySQLFindAll(@PathVariable int dataCount) {
        long time = performanceTestService.testMySQLFindAll(dataCount);
        return ResponseEntity.ok(time);
    }
}