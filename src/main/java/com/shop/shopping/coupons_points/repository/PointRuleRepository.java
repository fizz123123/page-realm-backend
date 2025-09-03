package com.shop.shopping.coupons_points.repository;

import com.shop.shopping.coupons_points.entity.PointRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRuleRepository extends JpaRepository<PointRule, Long> {
    Optional<PointRule> findTopByOrderByIdDesc();
}

