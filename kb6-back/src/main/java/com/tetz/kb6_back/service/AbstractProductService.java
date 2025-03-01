package com.tetz.kb6_back.service;

public interface AbstractProductService {
    void decreaseStock(Long productId, int quantity);
    long performanceTest(Long productId, int quantity, int threadCount);
}
