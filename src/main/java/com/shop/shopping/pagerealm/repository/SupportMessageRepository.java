package com.shop.shopping.pagerealm.repository;

import com.shop.shopping.pagerealm.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
