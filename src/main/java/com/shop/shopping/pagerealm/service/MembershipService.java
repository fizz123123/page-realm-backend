package com.shop.shopping.pagerealm.service;

import com.shop.shopping.pagerealm.dto.MembershipStatusDTO;

import java.time.LocalDateTime;

public interface MembershipService {
    void recordPurchase(Long userId, Integer amount, LocalDateTime occurredAt);
    MembershipStatusDTO getStatus(Long userId);
}

