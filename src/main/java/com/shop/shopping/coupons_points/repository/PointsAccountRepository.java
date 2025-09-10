package com.shop.shopping.coupons_points.repository;

import com.shop.shopping.coupons_points.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByUserId(Long userId);
}

