package com.example.repository;
import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author : Ze Li
 * @Date : 25/09/2025 21:24
 * @Version : V1.0
 * @Description :
 */

@Repository
public interface UserRepository extends JpaRepository<User, String> {
}
