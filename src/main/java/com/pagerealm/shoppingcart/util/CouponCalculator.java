package com.pagerealm.shoppingcart.util;

public class CouponCalculator {
    // 計算優惠劵折扣工具
    // 公式 ：IF discount_type = 'PERCENTAGE' THEN (total_amount * (discount_value / 100))
    //        ELSEIF discount_type = 'FIXED' THEN discount_value
    // 限制 ：不可超過最大折扣金額 max_discount_amount
    // 回傳折扣後的金額（int）
    public static int calculateDiscount(Integer totalAmount, String discountType, Integer discountValue, Integer maxDiscountAmount) {
        if (totalAmount == null || discountType == null || discountValue == null) return 0;
        int discount = 0;
        if (discountType.equals("PERCENT")) {
            // 百分比折扣
            discount = (int) Math.floor(totalAmount * (discountValue / 100.0));
        } else if (discountType.equals("FIXED")) {
            // 固定金額折扣
            discount = discountValue;
        }
        // 限制不可超過最大折扣金額
        if (maxDiscountAmount != null && discount > maxDiscountAmount) {
            discount = maxDiscountAmount;
        }
        return Math.max(discount, 0);
    }



}
