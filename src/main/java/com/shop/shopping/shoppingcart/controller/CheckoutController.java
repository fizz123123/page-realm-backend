package com.shop.shopping.shoppingcart.controller;

import com.shop.shopping.pagerealm.security.service.UserDetailsImpl;
import com.shop.shopping.shoppingcart.dto.request.ApplyCouponRequest;
import com.shop.shopping.shoppingcart.dto.request.ApplyPointsRequest;
import com.shop.shopping.shoppingcart.dto.response.*;
import com.shop.shopping.shoppingcart.service.CheckoutService;
import com.shop.shopping.shoppingcart.util.CheckMacValueUtil;
import com.shop.shopping.shoppingcart.util.payment.EcpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;
    private final EcpayClient ecpayClient;

    public CheckoutController(EcpayClient ecpayClient, CheckoutService checkoutService) {
        this.ecpayClient = ecpayClient;
        this.checkoutService = checkoutService;
    }

    @Value("${ecpay.hash-key}")
    private String hashKey;

    @Value("${ecpay.hash-iv}")
    private String hashIv;

    /**
     * 購物車小計 獲得點數
     * @param userDetails
     * @return
     */
    @GetMapping("/summary")
    public ResponseEntity<CartSummaryResponse> getCartSummary(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartSummaryResponse response = checkoutService.getCartSummary(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 會員資訊
     * @param userDetails
     * @return
     */
    @GetMapping("/membershipTier")
    public ResponseEntity<MembershipTierResponse> getMembershipTierInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        MembershipTierResponse response = checkoutService.getMembershipTierInfo(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 可用點數
     * @param userDetails
     * @return
     */
    @GetMapping("/pointsInfo")
    public ResponseEntity<PointsInfoResponse> getAvailablePoints(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        PointsInfoResponse response = checkoutService.getAvailablePoints(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 折抵點數
     * @param userDetails
     * @param request
     * @return
     */
    @PostMapping("/applyPoints")
    public ResponseEntity<PointsDeductionResponse> applyPoints(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                              @RequestBody ApplyPointsRequest request) {
        PointsDeductionResponse response = checkoutService.applyPoints(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 使用優惠劵
     * @param userDetails
     * @param request
     * @return
     */
    @PostMapping("/applyCoupon")
    public ResponseEntity<CouponDeductionResponse> applyCoupon(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                              @RequestBody ApplyCouponRequest request) {
        CouponDeductionResponse response = checkoutService.applyCoupon(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 結帳並建立訂單
     * @param userDetails
     * @return
     */
    @PostMapping(value = "/createOrder", produces = MediaType.TEXT_HTML_VALUE)
    public String createOrder(@AuthenticationPrincipal UserDetailsImpl userDetails) {

        Map<String, String> orderInfo = checkoutService.createOrder(userDetails.getId());

        String tradeNo = orderInfo.get("tradeNo") + UUID.randomUUID().toString().replace("-", "").substring(0, 5);;
        Integer amount = Integer.valueOf(orderInfo.get("amount"));
        String itemName = orderInfo.get("itemName");


        Map<String, String> params = ecpayClient.buildCheckoutParams(
                tradeNo, amount, itemName, "Order " + tradeNo);

        // 產生自動送出的 HTML 表單
        StringBuilder inputs = new StringBuilder();
        params.forEach((k, v) -> {
            inputs.append("<input type='hidden' name='").append(escapeHtml(k))
                    .append("' value='").append(escapeHtml(v)).append("'>");
        });

        return "<!doctype html><html><body onload='document.forms[0].submit()'>"
                + "<form method='post' action='" + ecpayClient.getCashierUrl() + "'>"
                + inputs
                + "<noscript><button type='submit'>Go to ECPay</button></noscript>"
                + "</form></body></html>";
    }

    private String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#x27;");
    }


    @PostMapping("/notify")
    @ResponseBody
    public String notifyPay(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> params = form.toSingleValueMap();
        params.forEach((k, v) -> System.out.println(k + "=" + v));

        String incomingMac = params.get("CheckMacValue");
        String computedMac = CheckMacValueUtil.generate(params,
                hashKey,
                hashIv);
        System.out.println("Incoming CheckMacValue:" + incomingMac);
        System.out.println("Computed CheckMacValue:" + computedMac);

        boolean valid = incomingMac != null && incomingMac.equalsIgnoreCase(computedMac);
        if (!valid) {
            return "0|ERROR";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(params.get("PaymentDate"), formatter);

        checkoutService.notify(params.get("MerchantTradeNo").substring(0, 15),params.get("PaymentType"),dateTime);


        return "1|OK";
    }


}
