package com.tetz.kb6_back.repository;

import com.tetz.kb6_back.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 낙관적 락은 @Version 어노테이션을 사용하므로 별도의 쿼리가 필요 없음
    Optional<Product> findById(Long id);

    // 비관적 락 (읽기)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticReadLock(@Param("id") Long id);

    // 비관적 락 (쓰기)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticWriteLock(@Param("id") Long id);
}