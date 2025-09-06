package com.pagerealm.authentication.service.impl;

import com.pagerealm.authentication.dto.MembershipStatusDTO;
import com.pagerealm.authentication.entity.MembershipTier;
import com.pagerealm.authentication.entity.User;
import com.pagerealm.authentication.repository.UserRepository;
import com.pagerealm.authentication.service.MembershipService;
import com.pagerealm.shoppingcart.util.MembershipUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
public class MembershipServiceImpl implements MembershipService {

    private final UserRepository userRepository;

    public MembershipServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipStatusDTO getStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        // 與 checkout 對齊：用 User 上的「當期視窗」與「累計金額」
        Integer windowTotal = user.getMembershipWindowTotal() == null ? 0 : user.getMembershipWindowTotal();
        LocalDateTime windowEnd = user.getMembershipWindowEnd();
        if (windowEnd == null) {
            // 若尚未初始化，落在本月月底 23:59:59
            YearMonth ym = YearMonth.now();
            windowEnd = ym.atEndOfMonth().atTime(23, 59, 59);
        }
        LocalDateTime windowStart = YearMonth.from(windowEnd).atDay(1).atStartOfDay();

        // 等級與返點（使用組內既有換算）
        MembershipTier current = MembershipUtils.fromAmount(windowTotal);
        MembershipTier next = MembershipUtils.next(current);
        Integer amountToNext = MembershipUtils.amountToNext(windowTotal);

        BigDecimal baseRate = MembershipTier.LV1.cashbackRate();
        BigDecimal totalRate = current.cashbackRate();
        BigDecimal bonusRate = totalRate.subtract(baseRate).max(BigDecimal.ZERO);

        // 顯示字串
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String periodText = dtf.format(windowStart) + " ~ " + dtf.format(windowEnd);
        String cashbackText = String.format("一般點數 %s%% + 獎勵點數 %s%% ，共 %s%%",
                percent(baseRate), percent(bonusRate), percent(totalRate));
        String nextUpgradeText = (next == null)
                ? "已是最高等級"
                : String.format("%s 前結帳金額再增加 %d 元，%s 即可升至 Lv %d",
                    dtf.format(windowEnd),
                    amountToNext,
                    formatYearMonth(YearMonth.from(windowEnd).plusMonths(1)),
                    next.level());

        // 組裝 DTO
        MembershipStatusDTO dto = new MembershipStatusDTO();
        dto.currentLevel = current.level();
        dto.currentTierName = current.displayName();

        dto.baseCashbackRate = baseRate;
        dto.bonusCashbackRate = bonusRate;
        dto.totalCashbackRate = totalRate;
        dto.cashbackRate = totalRate; // 相容舊欄位

        dto.windowStart = windowStart;
        dto.windowEnd = windowEnd;
        dto.windowTotal = windowTotal;

        dto.nextLevel = (next == null) ? null : next.level();
        dto.nextTierName = (next == null) ? null : next.displayName();
        dto.amountToNext = amountToNext;
        dto.nextCashbackRate = (next == null) ? null : next.cashbackRate();

        dto.periodText = periodText;
        dto.cashbackText = cashbackText;
        dto.nextUpgradeText = nextUpgradeText;

        return dto;
    }

    @Override
    @Transactional
    public void recordPurchase(Long userId, Integer amount, LocalDateTime when) {
        // 僅供本地／整合測試：直接累加到當期視窗，並按 checkout 規則更新等級
        if (amount == null || amount <= 0) return;
        User user = userRepository.findById(userId).orElseThrow();
        int total = (user.getMembershipWindowTotal() == null ? 0 : user.getMembershipWindowTotal()) + amount;
        user.setMembershipWindowTotal(total);
        user.setMembershipTier(MembershipUtils.fromAmount(total));
        userRepository.save(user);
    }

    private static String percent(BigDecimal rate) {
        BigDecimal v = rate.multiply(new BigDecimal("100"));
        v = v.stripTrailingZeros();
        if (v.scale() < 0) v = v.setScale(0);
        if (v.scale() > 2) v = v.setScale(2, java.math.RoundingMode.DOWN);
        return v.toPlainString();
    }

    private static String formatYearMonth(YearMonth ym) {
        return ym.getYear() + "/" + ym.getMonthValue();
    }
}