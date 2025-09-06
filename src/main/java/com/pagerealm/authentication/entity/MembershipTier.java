package com.pagerealm.authentication.entity;

import java.math.BigDecimal;
import java.util.Arrays;

public enum MembershipTier {
    LV1(1, 0,    new BigDecimal("0.01")),
    LV2(2, 500,  new BigDecimal("0.03")),
    LV3(3, 2000, new BigDecimal("0.05")),
    LV4(4, 5000,new BigDecimal("0.08")),
    LV5(5, 10000,new BigDecimal("0.12"));

    private final int level;
    private final int threshold;
    private final BigDecimal cashbackRate; // 0.01 = 1%

    MembershipTier(int level, int threshold, BigDecimal cashbackRate) {
        this.level = level;
        this.threshold = threshold;
        this.cashbackRate = cashbackRate;
    }

    public int level() { return level; }
    public int threshold() { return threshold; }
    public BigDecimal cashbackRate() { return cashbackRate; }

    public static MembershipTier fromAmount(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        MembershipTier result = LV1;
        for (MembershipTier t : values()) {
            if (amount.compareTo(BigDecimal.valueOf(t.threshold)) >= 0 && t.level >= result.level) {
                result = t;
            }
        }
        return result;
    }

    public MembershipTier next() {
        return Arrays.stream(values())
                .filter(t -> t.level == this.level + 1)
                .findFirst()
                .orElse(null);
    }



    public String displayName() {
        return "會員等級" + level;
    }
}
