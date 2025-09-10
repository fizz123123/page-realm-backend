package com.shop.shopping.shoppingcart.controller;

import com.shop.shopping.shoppingcart.util.payment.EcpayClient;
import com.shop.shopping.shoppingcart.util.CheckMacValueUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/ecpay")
public class EcpayController {
    private final EcpayClient ecpayClient;

    @Value("${ecpay.hash-key}")
    private String hashKey;

    @Value("${ecpay.hash-iv}")
    private String hashIV;

    public EcpayController(EcpayClient ecpayClient) {
        this.ecpayClient = ecpayClient;
    }

    @PostMapping(value = "/checkout", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String checkout(@RequestParam("amount") int amount,
                           @RequestParam("itemName") String itemName
                           ) {
        // 簡單做法：用 UUID 產生唯一訂單號（正式環境請用自己的規則）
        String tradeNo = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);

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

    // 綠界背景通知（付款完成/狀態更新會打這裡）——一定要回「1|OK」
    @PostMapping("/notify")
    @ResponseBody
    public String notifyPay(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> params = form.toSingleValueMap();
        params.forEach((k, v) -> System.out.println(k + "=" + v));

        // 取出 CheckMacValue 驗證
        String incomingMac = params.get("CheckMacValue");
        String computedMac = CheckMacValueUtil.generate(params,
                hashKey,
                hashIV);
        System.out.println("Incoming CheckMacValue:" + incomingMac);
        System.out.println("Computed CheckMacValue:" + computedMac);

        boolean valid = incomingMac != null && incomingMac.equalsIgnoreCase(computedMac);
        if (!valid) {
            // 驗簽失敗：記 log，不更新訂單
            return "0|ERROR";
        }

        // TODO: 根據 params.get("RtnCode"), "TradeNo", "MerchantTradeNo" 更新你資料庫訂單狀態
        // 成功後一定要回 "1|OK" 告知綠界「我收到了」
        return "1|OK";
    }

    // 使用者付款後導回頁（僅顯示用，狀態以 notify 為準）
    @GetMapping("/thank-you")
    @ResponseBody
    public String thankYou() {
        return "感謝您的訂購！若已成功付款，我們會盡快為您出貨。";
    }

    private String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#x27;");
    }

}
