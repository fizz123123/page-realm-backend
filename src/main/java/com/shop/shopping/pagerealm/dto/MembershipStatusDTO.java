package com.shop.shopping.pagerealm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MembershipStatusDTO {
    public int currentLevel;
    public String currentTierName;
    public BigDecimal cashbackRate; // 0.01 = 1%

    public LocalDateTime windowStart;
    public LocalDateTime windowEnd;
    public Integer windowTotal;

    public Integer nextLevel;            // 可能為 null (已是最高)
    public String nextTierName;          // 可能為 null
    public Integer amountToNext;      // 若為最高等級則為 0
    public BigDecimal nextCashbackRate;  // 可能為 null
}
