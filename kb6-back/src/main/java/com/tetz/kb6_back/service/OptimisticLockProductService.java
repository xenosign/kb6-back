package com.tetz.kb6_back.service;

import com.tetz.kb6_back.entity.Product;
import com.tetz.kb6_back.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockProductService implements AbstractProductService {

    private final ProductRepository productRepository;

    // 중요: 트랜잭션 경계를 메서드 호출마다 명확하게 설정
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long productId, int quantity) {
        boolean success = false;
        int retryCount = 0;
        final int MAX_RETRY = 5;

        while (!success && retryCount < MAX_RETRY) {
            try {
                // 최신 상태 조회
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                if (product.getStock() < quantity) {
                    throw new RuntimeException("Not enough stock");
                }

                // 재고 감소
                product.setStock(product.getStock() - quantity);

                // 즉시 저장 및 flush
                productRepository.saveAndFlush(product);
                success = true;

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.info("Optimistic lock exception occurred. Retry count: {}", retryCount);

                // 재시도 전 딜레이
                try {
                    // 단순한 고정 딜레이 사용
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted", ie);
                }
            }
        }

        if (!success) {
            throw new RuntimeException("Failed to decrease stock after " + MAX_RETRY + " retries");
        }
    }

    @Override
    public long performanceTest(Long productId, int quantity, int threadCount) {
        // 트랜잭션 경계 밖에서 실행
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 스레드 수를 제한하여 실행 - 모든 스레드를 동시에 실행하지 않음
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    // 각 스레드 약간 지연 시작
                    if (threadNum % 10 == 0 && threadNum > 0) {
                        Thread.sleep(50);
                    }

                    // 각 스레드는 독립적인 트랜잭션으로 실행
                    decreaseStock(productId, quantity);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("Error in optimistic lock thread: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        log.info("Optimistic lock test completed. Duration: {}ms, Success: {}, Fail: {}",
                duration, successCount.get(), failCount.get());

        return duration;
    }
}