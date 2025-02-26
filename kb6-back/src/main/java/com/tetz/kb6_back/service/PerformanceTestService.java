package com.tetz.kb6_back.service;

import com.tetz.kb6_back.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceTestService {
    private final RedisService redisService;
    private final UserSQLService userService;

    // 테스트 데이터 생성
    public List<UserDto> generateTestUsers(int count) {
        List<UserDto> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString();
            users.add(new UserDto(id, "User " + i, 20 + i % 40));
        }
        return users;
    }

    // Redis에 데이터 저장 성능 테스트
    public long testRedisSave(List<UserDto> users) {
        long startTime = System.nanoTime();

        for (UserDto user : users) {
            redisService.saveUser(user);
        }

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    // MySQL에 데이터 저장 성능 테스트
    public long testMySQLSave(List<UserDto> users) {
        long startTime = System.nanoTime();

        for (UserDto user : users) {
            userService.saveUser(user);
        }

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    // Redis에서 데이터 조회 성능 테스트
    public long testRedisRead(List<UserDto> users) {
        // 먼저 데이터 저장
        for (UserDto user : users) {
            redisService.saveUser(user);
        }

        long startTime = System.nanoTime();

        for (UserDto user : users) {
            redisService.getUser(user.getId());
        }

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    // MySQL에서 데이터 조회 성능 테스트
    public long testMySQLRead(List<UserDto> users) {
        // 먼저 데이터 저장
        for (UserDto user : users) {
            userService.saveUser(user);
        }

        long startTime = System.nanoTime();

        for (UserDto user : users) {
            userService.getUser(user.getId());
        }

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    // 벌크 작업 테스트
    public long testRedisBulkRead(int count) {
        List<UserDto> users = generateTestUsers(count);

        // 먼저 데이터 저장
        for (UserDto user : users) {
            redisService.saveUser(user);
        }

        long startTime = System.nanoTime();

        // 여기서는 단순 조회만 테스트
        for (UserDto user : users) {
            redisService.getUser(user.getId());
        }

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    public long testMySQLBulkRead(int count) {
        List<UserDto> users = generateTestUsers(count);

        // 먼저 데이터 저장
        for (UserDto user : users) {
            userService.saveUser(user);
        }

        long startTime = System.nanoTime();

        // 개별 조회
        for (UserDto user : users) {
            userService.getUser(user.getId());
        }

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    // MySQL의 전체 조회 성능 테스트
    public long testMySQLFindAll(int count) {
        List<UserDto> users = generateTestUsers(count);

        // 먼저 데이터 저장
        for (UserDto user : users) {
            userService.saveUser(user);
        }

        long startTime = System.nanoTime();

        userService.getAllUsers();

        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    // 통합 테스트 실행 및 결과 출력
    public void runComparisonTest(int dataCount) {
        List<UserDto> testUsers = generateTestUsers(dataCount);

        log.info("시작: Redis vs MySQL 성능 비교 테스트 (데이터 수: {})", dataCount);

        // 저장 성능 테스트
        long redisSaveTime = testRedisSave(testUsers);
        long mysqlSaveTime = testMySQLSave(testUsers);

        log.info("Redis 저장 시간: {} ms", redisSaveTime);
        log.info("MySQL 저장 시간: {} ms", mysqlSaveTime);
        log.info("저장 속도 비율: MySQL/Redis = {}", (double)mysqlSaveTime / redisSaveTime);

        // 조회 성능 테스트
        long redisReadTime = testRedisRead(testUsers);
        long mysqlReadTime = testMySQLRead(testUsers);

        log.info("Redis 조회 시간: {} ms", redisReadTime);
        log.info("MySQL 조회 시간: {} ms", mysqlReadTime);
        log.info("조회 속도 비율: MySQL/Redis = {}", (double)mysqlReadTime / redisReadTime);

        // 대량 데이터 조회 테스트
        log.info("대량 데이터 개별 조회 테스트 시작...");
        long redisBulkTime = testRedisBulkRead(dataCount);
        long mysqlBulkTime = testMySQLBulkRead(dataCount);

        log.info("Redis 대량 조회 시간: {} ms", redisBulkTime);
        log.info("MySQL 대량 조회 시간: {} ms", mysqlBulkTime);
        log.info("대량 조회 속도 비율: MySQL/Redis = {}", (double)mysqlBulkTime / redisBulkTime);

        // MySQL 전체 조회 테스트
        log.info("MySQL 전체 조회 테스트 시작...");
        long mysqlFindAllTime = testMySQLFindAll(dataCount);
        log.info("MySQL 전체 조회 시간: {} ms", mysqlFindAllTime);

        log.info("테스트 완료!");
    }
}
