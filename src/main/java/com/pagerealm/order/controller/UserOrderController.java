package com.pagerealm.order.controller;

import com.pagerealm.authentication.security.service.UserDetailsImpl;
import com.pagerealm.order.dto.UserOrderResponse;
import com.pagerealm.shoppingcart.repository.OrdersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/order")
public class UserOrderController {
    private final OrdersRepository ordersRepository;

    public  UserOrderController(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;

    }

    /**
     * 取得用戶歷史訂單
     * @param userDetails
     * @return
     */
    @GetMapping("/view")
    public ResponseEntity<Page<UserOrderResponse>> getUserOrders(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                 @RequestParam int page,
                                                                 @RequestParam(defaultValue = "5") int size
    )
    {
        Page<UserOrderResponse> responses = ordersRepository.findAllByUserIdOrderByCreatedAtDesc(userDetails.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(responses);
    }

}
