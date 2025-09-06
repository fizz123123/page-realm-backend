package com.pagerealm.authentication.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MembershipStatusDTO {
    public int currentLevel;
    public String currentTierName;

    // 返點比例
    public BigDecimal baseCashbackRate;   // 一般點數（Lv1）
    public BigDecimal bonusCashbackRate;  // 獎勵點數（current - base）
    public BigDecimal totalCashbackRate;  // 共（current）
    // 相容舊欄位（= totalCashbackRate）
    public BigDecimal cashbackRate;

    // 當期視窗與累計金額
    public LocalDateTime windowStart;
    public LocalDateTime windowEnd;
    public Integer windowTotal;

    // 下一級資訊
    public Integer nextLevel;             // 可能為 null（最高級）
    public String nextTierName;           // 可能為 null
    public Integer amountToNext;          // 最高級則為 0
    public BigDecimal nextCashbackRate;   // 可能為 null

    // 前端直接顯示字串
    public String periodText;             // yyyy-MM-dd HH:mm:ss ~ yyyy-MM-dd HH:mm:ss
    public String cashbackText;           // 一般點數 x% + 獎勵點數 y% ，共 z%
    public String nextUpgradeText;        // 2025/MM/dd 前結帳金額再增加 N 元，YYYY/M 即可升至 Lv X
}
