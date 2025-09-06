package com.pagerealm.authentication.service;

import com.pagerealm.authentication.dto.MembershipStatusDTO;

import java.time.LocalDateTime;

public interface MembershipService {
    void recordPurchase(Long userId, Integer amount, LocalDateTime occurredAt);
    MembershipStatusDTO getStatus(Long userId);
}

