package com.example.thinkmap.domain.repository;

import com.example.thinkmap.domain.entity.AuthProvider;
import com.example.thinkmap.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
    Optional<User> findByEmail(String email);
}
