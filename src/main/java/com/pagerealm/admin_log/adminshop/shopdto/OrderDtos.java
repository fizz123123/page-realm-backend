package com.pagerealm.admin_log.adminshop.shopdto;

import com.pagerealm.shoppingcart.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderListItem {
        private Long id;
        private String orderNo;
        private Long userId;
        private Integer totalAmount;
        private Integer discountAmount;
        private Integer pointsDeductionAmount;
        private Integer payableAmount;
        private OrderStatus status;
        private String paymentType;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetail {
        private Long id;
        private Long bookId;
        private String name;
        private Integer price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetail {
        private Long id;
        private String orderNo;
        private Long userId;
        private Integer totalAmount;
        private Integer discountAmount;
        private Integer pointsDeductionAmount;
        private Integer payableAmount;
        private OrderStatus status;
        private String paymentType;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private List<OrderItemDetail> items;
    }
}

