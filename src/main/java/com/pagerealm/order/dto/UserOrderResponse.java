package com.pagerealm.order.dto;

import com.pagerealm.shoppingcart.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter @Setter @ToString
public class UserOrderResponse {
    private String orderNo;
    private LocalDateTime paidAt;
    private String status;
    private Integer payableAmount;
    private String paymentType;

    public UserOrderResponse(String orderNo,
                             LocalDateTime paidAt,
                             OrderStatus status,
                             Integer payableAmount,
                             String paymentType) {
        this.orderNo = orderNo;
        this.paidAt = paidAt;
        this.status = status.toString();
        this.payableAmount = payableAmount;
        this.paymentType = paymentType;
    }
}
