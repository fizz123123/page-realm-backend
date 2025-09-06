package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.entity.PointReservations;
import com.pagerealm.shoppingcart.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointReservationsRepository extends JpaRepository<PointReservations, Long> {

    Optional<PointReservations> findByUserIdAndStatus(Long userId, ReservationStatus status);
}
