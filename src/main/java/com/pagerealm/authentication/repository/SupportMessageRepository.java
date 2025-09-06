package com.pagerealm.authentication.repository;

import com.pagerealm.authentication.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
