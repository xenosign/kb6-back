package com.tetz.kb6_back.service;

import com.tetz.kb6_back.entity.Product;
import com.tetz.kb6_back.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
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

    @Override
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        boolean success = false;
        int retryCount = 0;
        final int MAX_RETRY = 5;

        while (!success && retryCount < MAX_RETRY) {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                if (product.getStock() < quantity) {
                    throw new RuntimeException("Not enough stock");
                }

                product.setStock(product.getStock() - quantity);
                productRepository.save(product);
                success = true;

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.info("Optimistic lock exception occurred. Retry count: {}", retryCount);

                // 재시도 전에 짧은 딜레이 추가
                try {
                    Thread.sleep(50);
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
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
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
