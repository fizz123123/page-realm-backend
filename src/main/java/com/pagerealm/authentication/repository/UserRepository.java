package com.pagerealm.authentication.repository;

import com.pagerealm.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUserName(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);

    void deleteByEmail(String email);
}
