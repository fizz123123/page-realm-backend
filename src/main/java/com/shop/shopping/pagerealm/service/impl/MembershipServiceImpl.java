package com.shop.shopping.pagerealm.service.impl;

import com.shop.shopping.pagerealm.dto.MembershipStatusDTO;
import com.shop.shopping.pagerealm.entity.MembershipTier;
import com.shop.shopping.pagerealm.entity.User;
import com.shop.shopping.pagerealm.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.shop.shopping.shoppingcart.util.MembershipUtils.fromAmount;

@Service
public class MembershipServiceImpl implements com.shop.shopping.pagerealm.service.MembershipService {

    private final UserRepository userRepository;

    public MembershipServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void recordPurchase(Long userId, Integer amount, LocalDateTime occurredAt) {
        Objects.requireNonNull(userId, "userId");
        if (amount == null) amount = 0;
        if (occurredAt == null) occurredAt = LocalDateTime.now();

        User user = userRepository.findById(userId).orElseThrow();

        // 若視窗已結束，先結算上一期
        finalizeWindowIfEnded(user, occurredAt);

        // 尚未開窗 -> 以本次消費時間當起點，30 天視窗
        if (user.getMembershipWindowStart() == null) {
            user.setMembershipWindowStart(occurredAt);
            user.setMembershipWindowEnd(occurredAt.plusDays(30).minusSeconds(1));
            user.setMembershipWindowTotal(0);
        }

        // 視窗內累積金額
        if (!occurredAt.isAfter(user.getMembershipWindowEnd())) {
            user.setMembershipWindowTotal(user.getMembershipWindowTotal()+amount);
        } else {
            // 若消費已跨期，先結算上一期再開新視窗並計入本次金額
            applyLevelUpAndReset(user);
            user.setMembershipWindowStart(occurredAt);
            user.setMembershipWindowEnd(occurredAt.plusDays(30).minusSeconds(1));
            user.setMembershipWindowTotal(amount);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public MembershipStatusDTO getStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        finalizeWindowIfEnded(user, LocalDateTime.now());
        user = userRepository.save(user);

        MembershipStatusDTO dto = new MembershipStatusDTO();
        dto.currentLevel = user.getMembershipTier().level();
        dto.currentTierName = user.getMembershipTier().displayName();
        dto.cashbackRate = user.getMembershipTier().cashbackRate();

        dto.windowStart = user.getMembershipWindowStart();
        dto.windowEnd = user.getMembershipWindowEnd();
        dto.windowTotal = user.getMembershipWindowTotal();

        MembershipTier next = user.getMembershipTier().next();
        if (next != null) {
            dto.nextLevel = next.level();
            dto.nextTierName = next.displayName();
            dto.nextCashbackRate = next.cashbackRate();
            Integer base = user.getMembershipWindowTotal() == null ? 0 : user.getMembershipWindowTotal();
            int diff = next.threshold() - base;
            dto.amountToNext = diff > 0 ? diff : 0;
        } else {
            dto.nextLevel = null;
            dto.nextTierName = null;
            dto.nextCashbackRate = null;
            dto.amountToNext = 0;
        }
        return dto;
    }

    private void applyLevelUpAndReset(User user) {
        Integer total = user.getMembershipWindowTotal() == null ? 0 : user.getMembershipWindowTotal();
        MembershipTier target = fromAmount(total);
        if (target.level() > user.getMembershipTier().level()) {
            user.setMembershipTier(target);
        }
        user.setMembershipWindowStart(null);
        user.setMembershipWindowEnd(null);
        user.setMembershipWindowTotal(0);
    }

    private void finalizeWindowIfEnded(User user, LocalDateTime now) {
        if (user.getMembershipWindowEnd() != null && now.isAfter(user.getMembershipWindowEnd())) {
            applyLevelUpAndReset(user);
        }
    }
}

