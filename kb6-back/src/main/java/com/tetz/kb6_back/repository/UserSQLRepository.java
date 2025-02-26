package com.tetz.kb6_back.repository;

import com.tetz.kb6_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSQLRepository extends JpaRepository<User, String> {
}
