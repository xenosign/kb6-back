package com.tetz.kb6_back.service;

import com.tetz.kb6_back.entity.Product;
import com.tetz.kb6_back.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PessimisticLockProductService implements AbstractProductService {

    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate; // 추가: 프로그래밍 방식 트랜잭션 관리

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long productId, int quantity) {
        // 비관적 락을 사용하여 상품 조회
        Product product = productRepository.findByIdWithPessimisticWriteLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() < quantity) {
            throw new RuntimeException("Not enough stock");
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
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
                    // 프로그래밍 방식으로 트랜잭션을 시작하여 각 스레드가 명시적인 트랜잭션 내에서 실행되도록 함
                    transactionTemplate.execute(status -> {
                        try {
                            decreaseStock(productId, quantity);
                            return true;
                        } catch (Exception e) {
                            status.setRollbackOnly();
                            throw e;
                        }
                    });

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("Error in pessimistic lock thread: {}", e.getMessage());
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

        log.info("Pessimistic lock test completed. Duration: {}ms, Success: {}, Fail: {}",
                duration, successCount.get(), failCount.get());

        return duration;
    }
}