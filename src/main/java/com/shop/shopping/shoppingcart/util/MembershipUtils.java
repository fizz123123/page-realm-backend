package com.shop.shopping.shoppingcart.util;

import com.shop.shopping.pagerealm.entity.MembershipTier;

import java.util.Arrays;

public class MembershipUtils {
    /**
     * 根據消費金額，取得對應的會員等級
     */
    public static MembershipTier fromAmount(Integer amount) {
        if (amount == null) amount = 0;
        MembershipTier result = MembershipTier.LV1;
        for (MembershipTier t : MembershipTier.values()) {
            if (amount >= t.threshold() && t.level() >= result.level()) {
                result = t;
            }
        }
        return result;
    }


    /**
     * 取得下一個等級
     */
    public static MembershipTier next(MembershipTier current) {
        return Arrays.stream(MembershipTier.values())
                .filter(t -> t.level() == current.level() + 1)
                .findFirst()
                .orElse(null);
    }
    /**
     * 計算還差多少金額才能升級
     */
    public static Integer amountToNext(Integer amount) {
        if (amount == null) amount = 0;
        MembershipTier current = fromAmount(amount);
        MembershipTier next = next(current);
        if (next == null) return 0; // 已經是最高等級
        int diff = next.threshold() - amount;
        return diff > 0 ? diff : 0;
    }
}

