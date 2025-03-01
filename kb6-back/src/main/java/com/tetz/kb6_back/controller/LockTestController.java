package com.tetz.kb6_back.controller;

import com.tetz.kb6_back.entity.Product;
import com.tetz.kb6_back.repository.ProductRepository;
import com.tetz.kb6_back.service.OptimisticLockProductService;
import com.tetz.kb6_back.service.PessimisticLockProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lock-test")
@RequiredArgsConstructor
@Slf4j
public class LockTestController {

    private final OptimisticLockProductService optimisticLockService;
    private final PessimisticLockProductService pessimisticLockService;
    private final ProductRepository productRepository;

    @PostMapping("/init")
    public ResponseEntity<Product> initProduct(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        int stock = (int) request.get("stock");

        Product product = Product.builder()
                .name(name)
                .stock(stock)
                .version(0L)
                .build();

        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(savedProduct);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return ResponseEntity.ok(product);
    }

    @PostMapping("/test/optimistic")
    public ResponseEntity<Map<String, Object>> testOptimisticLock(
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestParam int threadCount) {

        // 테스트 시작 전 상품 정보 저장
        Product beforeTest = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int initialStock = beforeTest.getStock();

        // 테스트 수행
        long duration = optimisticLockService.performanceTest(productId, quantity, threadCount);

        // 테스트 후 상품 정보 조회
        Product afterTest = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("lockType", "Optimistic");
        result.put("duration", duration);
        result.put("initialStock", initialStock);
        result.put("finalStock", afterTest.getStock());
        result.put("expectedFinalStock", initialStock - (quantity * threadCount));
        result.put("isConsistent", (initialStock - (quantity * threadCount) == afterTest.getStock()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/test/pessimistic")
    public ResponseEntity<Map<String, Object>> testPessimisticLock(
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestParam int threadCount) {

        // 테스트 시작 전 상품 정보 저장
        Product beforeTest = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int initialStock = beforeTest.getStock();

        // 테스트 수행
        long duration = pessimisticLockService.performanceTest(productId, quantity, threadCount);

        // 테스트 후 상품 정보 조회
        Product afterTest = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("lockType", "Pessimistic");
        result.put("duration", duration);
        result.put("initialStock", initialStock);
        result.put("finalStock", afterTest.getStock());
        result.put("expectedFinalStock", initialStock - (quantity * threadCount));
        result.put("isConsistent", (initialStock - (quantity * threadCount) == afterTest.getStock()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareLockPerformance(
            @RequestParam int stock,
            @RequestParam int quantity,
            @RequestParam int threadCount) {

        // 낙관적 락 테스트용 상품 생성
        Product optimisticProduct = Product.builder()
                .name("Optimistic Lock Test Product")
                .stock(stock)
                .version(0L)
                .build();
        optimisticProduct = productRepository.save(optimisticProduct);

        // 비관적 락 테스트용 상품 생성
        Product pessimisticProduct = Product.builder()
                .name("Pessimistic Lock Test Product")
                .stock(stock)
                .version(0L)
                .build();
        pessimisticProduct = productRepository.save(pessimisticProduct);

        // 낙관적 락 테스트 수행
        long optimisticDuration = optimisticLockService.performanceTest(
                optimisticProduct.getId(), quantity, threadCount);

        // 비관적 락 테스트 수행
        long pessimisticDuration = pessimisticLockService.performanceTest(
                pessimisticProduct.getId(), quantity, threadCount);

        // 테스트 결과 조회
        Product optimisticAfterTest = productRepository.findById(optimisticProduct.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Product pessimisticAfterTest = productRepository.findById(pessimisticProduct.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("optimisticLockDuration", optimisticDuration);
        result.put("pessimisticLockDuration", pessimisticDuration);
        result.put("optimisticFinalStock", optimisticAfterTest.getStock());
        result.put("pessimisticFinalStock", pessimisticAfterTest.getStock());
        result.put("expectedFinalStock", stock - (quantity * threadCount));
        result.put("optimisticIsConsistent",
                (stock - (quantity * threadCount) == optimisticAfterTest.getStock()));
        result.put("pessimisticIsConsistent",
                (stock - (quantity * threadCount) == pessimisticAfterTest.getStock()));
        result.put("faster", optimisticDuration < pessimisticDuration ? "Optimistic" : "Pessimistic");

        return ResponseEntity.ok(result);
    }
}