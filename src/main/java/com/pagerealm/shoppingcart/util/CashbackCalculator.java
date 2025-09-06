package com.pagerealm.shoppingcart.util;

import com.pagerealm.authentication.entity.MembershipTier;
import com.pagerealm.coupons_points.entity.PointRule;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CashbackCalculator {
    // 計算回饋點數工具
    // 公式： 消費金額 * (會員等級回饋比例 + point_rules 回饋比例)
    // 回傳最終可獲得點數（int）
    public static int calculatePoints(Integer amount, MembershipTier tier, PointRule rule) {
        if (amount == null || tier == null || rule == null) return 0;
        BigDecimal tierRate = tier.cashbackRate();
        // point_rules 回饋比率 basis points (基點) ，所以是100 ÷ 10000 = 0.01 = 1%
        BigDecimal ruleRate = BigDecimal.valueOf(rule.getRewardRateBp()).divide(BigDecimal.valueOf(10000));
        // 計算
        BigDecimal points = BigDecimal.valueOf(amount).multiply(tierRate.add(ruleRate));
        // 無條件捨去小數點 ROUND_DOWN
        int result = points.setScale(0, RoundingMode.DOWN).intValue();
        if (rule.getMaxRewardPoints() != null && result > rule.getMaxRewardPoints()) {
            result = rule.getMaxRewardPoints();
        }
        return Math.max(result, 0);
    }
}