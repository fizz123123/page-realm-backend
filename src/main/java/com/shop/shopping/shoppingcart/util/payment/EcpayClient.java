package com.shop.shopping.shoppingcart.util.payment;

import com.shop.shopping.shoppingcart.util.CheckMacValueUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EcpayClient {
    @Value("${ecpay.merchant-id}")
    private String merchantId;

    @Value("${ecpay.hash-key}")
    private String hashKey;

    @Value("${ecpay.hash-iv}")
    private String hashIv;

    @Value("${ecpay.cashier-url}")
    private String cashierUrl;


    @Value("${ecpay.return-url}")
    private String returnURL;
    /*
    @Value("${ecpay.client-back-url}")
    private String clientBackURL;
    */

    private static final DateTimeFormatter TRADE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public String getCashierUrl() {
        return cashierUrl;
    }

    public Map<String, String> buildCheckoutParams(String tradeNo, int amount, String itemName, String tradeDesc) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", tradeNo);
        params.put("MerchantTradeDate", LocalDateTime.now().format(TRADE_DATE_FMT));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(amount));
        params.put("TradeDesc", tradeDesc);
        params.put("ItemName", itemName);
        params.put("ReturnURL", returnURL);
        params.put("ChoosePayment", "ALL");
        params.put("IgnorePayment", "BARCODE#ApplePay#TWQR#BNPL#WeiXin");
        params.put("EncryptType", "1");

        System.out.println(params);
        String checkMac = CheckMacValueUtil.generate(params, hashKey, hashIv);
        params.put("CheckMacValue", checkMac);
        return params;
    }
}