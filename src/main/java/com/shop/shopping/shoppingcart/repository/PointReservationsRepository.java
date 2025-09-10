package com.shop.shopping.shoppingcart.repository;

import com.shop.shopping.shoppingcart.entity.PointReservations;
import com.shop.shopping.shoppingcart.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointReservationsRepository extends JpaRepository<PointReservations, Long> {

    Optional<PointReservations> findByUserIdAndStatus(Long userId, ReservationStatus status);
}
