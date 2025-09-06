package com.pagerealm.shoppingcart.service.impl;

import com.pagerealm.coupons_points.entity.*;
import com.pagerealm.coupons_points.repository.*;
import com.pagerealm.shoppingcart.dto.response.*;
import com.pagerealm.shoppingcart.entity.*;
import com.pagerealm.shoppingcart.repository.*;
import com.pagerealm.authentication.entity.MembershipTier;
import com.pagerealm.authentication.entity.User;
import com.pagerealm.authentication.repository.UserRepository;
import com.pagerealm.shoppingcart.dto.request.ApplyCouponRequest;
import com.pagerealm.shoppingcart.dto.request.ApplyPointsRequest;
import com.pagerealm.shoppingcart.service.CheckoutService;
import com.pagerealm.shoppingcart.util.CashbackCalculator;
import com.pagerealm.shoppingcart.util.CouponCalculator;
import com.pagerealm.shoppingcart.util.payment.EcpayClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.pagerealm.shoppingcart.util.MembershipUtils.*;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    private UserRepository userRepository;
    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private OrdersRepository ordersRepository;
    private OrderItemsRepository orderItemsRepository;
    private PointsAccountRepository pointsAccountRepository;
    private PointLotRepository pointLotRepository;
    private PointsLedgerRepository pointsLedgerRepository;
    private PointRuleRepository pointRuleRepository;
    private PointReservationsRepository pointReservationsRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final EcpayClient ecpayClient;

    public CheckoutServiceImpl(UserRepository userRepository,
                               CartRepository cartRepository,
                               CartItemRepository cartItemRepository,
                               OrdersRepository ordersRepository,
                               OrderItemsRepository orderItemsRepository,
                               PointsAccountRepository pointsAccountRepository,
                               PointLotRepository pointLotRepository,
                               PointsLedgerRepository pointsLedgerRepository,
                               PointRuleRepository pointRuleRepository,
                               PointReservationsRepository pointReservationsRepository,
                               CouponRepository couponRepository,
                               CouponRedemptionRepository couponRedemptionRepository, EcpayClient ecpayClient
    ) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.pointsAccountRepository = pointsAccountRepository;
        this.pointLotRepository = pointLotRepository;
        this.pointsLedgerRepository = pointsLedgerRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.pointReservationsRepository = pointReservationsRepository;
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.ecpayClient = ecpayClient;
    }

    /**
     * 取得購物車小計、本次結帳後可以獲得的點數
     *
     * @param userId
     * @return
     */
    @Override
    public CartSummaryResponse getCartSummary(Long userId) {
        CartSummaryResponse resp = new CartSummaryResponse();
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到購物車");
        }
        Long cartId = cart.getId();
        List<CartItem> items = cartItemRepository.findByCart_Id(cartId);
        if (items == null) {
            throw new IllegalArgumentException("找不到購物車內容");
        }
        int total = items.stream()
                .mapToInt(item -> item.getSnapshotPrice() != null ? item.getSnapshotPrice() : 0)
                .sum();
        // 從user取得membership_tier 會員等級
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("找不到用戶資料");
        }
        MembershipTier tier = user.getMembershipTier();

        // 取得點數規則 取第一筆規則
        PointRule rule = pointRuleRepository.findTopByOrderByIdDesc().orElse(null);
        if (rule == null) {
            throw new IllegalArgumentException("找不到點數規則");
        }
        int points = CashbackCalculator.calculatePoints(total, tier, rule);

        resp.setCartTotalAmount(total);
        resp.setExpectedPointsReward(points);
        return resp;
    }

    /**
     * 取得會員等級資訊
     *
     * @param userId
     * @return
     */
    @Override
    public MembershipTierResponse getMembershipTierInfo(Long userId) {
        MembershipTierResponse resp = new MembershipTierResponse();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("找不到用戶");
        }

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到購物車");
        }
        Long cartId = cart.getId();
        List<CartItem> items = cartItemRepository.findByCart_Id(cartId);
        if (items == null) {
            throw new IllegalArgumentException("找不到購物車內容");
        }
        int total = items.stream()
                .mapToInt(item -> item.getSnapshotPrice() != null ? item.getSnapshotPrice() : 0)
                .sum();
        int membertotal = user.getMembershipWindowTotal();
        int sumtotal = membertotal + total;


        MembershipTier tier = fromAmount(sumtotal);
        MembershipTier nextTier = next(tier);
        Integer amountToNext = amountToNext(sumtotal);

        resp.setCurrentTier(tier.displayName());
        resp.setTierExpireAt(user.getMembershipWindowEnd());
        resp.setAmountToNextTier(amountToNext);
        resp.setNextTier(nextTier.displayName());
        return resp;
    }

    /**
     * 顯示可用點數
     * @param userId
     * @return
     */
    @Override
    public PointsInfoResponse getAvailablePoints(Long userId) {
        PointsAccount account = pointsAccountRepository.findById(userId).orElse(null);
        PointsInfoResponse resp = new PointsInfoResponse();
        if (account == null) {
            resp.setAvailablePoints(0);
            return resp;
        }
        resp.setAvailablePoints(account.getBalance());
        return resp;
    }

    /**
     * 使用點數
     * @param userId
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsDeductionResponse applyPoints(Long userId, ApplyPointsRequest request) {
        PointsAccount account = pointsAccountRepository.findById(userId).orElse(null);
        PointsDeductionResponse resp = new PointsDeductionResponse();

        if (account == null) {
            throw new IllegalArgumentException("找不到點數帳戶");
        }
        int pointsToApply = request.getPointsToApply();
        if (pointsToApply <= 0) {
            throw new NoSuchElementException("不可為空");
        }

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到購物車");
        }
        Long cartId = cart.getId();
        List<CartItem> items = cartItemRepository.findByCart_Id(cartId);
        int cartTotal = items.stream()
                .mapToInt(item -> item.getSnapshotPrice() != null ? item.getSnapshotPrice() : 0)
                .sum();

        PointRule rule = pointRuleRepository.findTopByOrderByIdDesc().orElse(null);
        if (rule == null) {
            throw new IllegalArgumentException("找不到點數規則");
        }
        Integer maxRedeemRatioBp = rule.getMaxRedeemRatioBp();
        int redeemRate = rule.getRedeemRate().intValue();
        // 最大可折抵金額
        int maxDeductAmount = cartTotal * maxRedeemRatioBp / 10000;
        // 最大可折抵點數
        int maxDeductPoints = maxDeductAmount / redeemRate;
        // 實際可用點數
        int availablePoints = account.getBalance();
        int actualPoints = Math.min(pointsToApply, Math.min(availablePoints, maxDeductPoints));
        int actualDeductAmount = actualPoints * redeemRate;

        if(pointReservationsRepository.findByUserIdAndStatus(userId, ReservationStatus.ACTIVE).isPresent()) {
            PointReservations reservation = pointReservationsRepository.findByUserIdAndStatus(userId, ReservationStatus.ACTIVE).orElse(null);
            if (reservation == null) {
                throw new IllegalArgumentException("找不到點數鎖定紀錄");
            }
            int oldPoints = reservation.getReservedPts();
            int balance = account.getBalance()+oldPoints-actualPoints;
            account.setBalance(balance);
            pointsAccountRepository.save(account);

            resp.setPointsApplied(actualPoints);
            resp.setDeductionAmount(actualDeductAmount);
            reservation.setReservedPts(actualPoints);
            reservation.setDeductionAmount(actualDeductAmount);



            pointReservationsRepository.save(reservation);

            return resp;
        }
        resp.setPointsApplied(actualPoints);
        resp.setDeductionAmount(actualDeductAmount);

        PointReservations reservation = new PointReservations();
        reservation.setUserId(userId);
        reservation.setReservedPts(actualPoints);
        reservation.setDeductionAmount(actualDeductAmount);
        reservation.setStatus(ReservationStatus.ACTIVE);

        int balance = account.getBalance()-actualPoints;
        account.setBalance(balance);
        pointsAccountRepository.save(account);

        pointReservationsRepository.save(reservation);

        return resp;
    }

    /**
     * 使用優惠券
     * @param userId
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponDeductionResponse applyCoupon(Long userId, ApplyCouponRequest request) {
        CouponDeductionResponse resp = new CouponDeductionResponse();
        resp.setCouponCode(request.getCouponCode());
        resp.setResult(false);
        String code = request.getCouponCode();

        if (code == null || code.isEmpty()) {
            throw new NoSuchElementException("不可為空");
        }

        Coupon coupon = couponRepository.findByGenericCodeIgnoreCase(code).orElse(null);
        if (coupon == null) {
            resp.setMessage("優惠券不存在");
            return  resp;
        }
        if (coupon.getStatus() != Coupon.CouponStatus.ACTIVE) {
            resp.setMessage("優惠券未啟用");
            return  resp;
        }
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到購物車");
        }
        Long cartId = cart.getId();
        List<CartItem> items = cartItemRepository.findByCart_Id(cartId);
        int cartTotal = items.stream()
                .mapToInt(item -> item.getSnapshotPrice() != null ? item.getSnapshotPrice() : 0)
                .sum();
        if (coupon.getMinSpendAmount() != null && cartTotal < coupon.getMinSpendAmount()) {
            resp.setMessage("未達優惠券最低消費門檻");
            return  resp;
        }

        if (coupon.getTotalUsageLimit() != null && coupon.getTotalUsageLimit() > 0) {
            long used = couponRedemptionRepository.countByCouponIdAndStatus(coupon.getId(), CouponRedemption.RedemptionStatus.APPLIED);
            if (used >= coupon.getTotalUsageLimit()) {
                resp.setMessage("優惠券已達全體使用上限");
                return  resp;
            }
        }
        if (coupon.getPerUserLimit() != null && coupon.getPerUserLimit() > 0) {
            long userUsed = couponRedemptionRepository.countByCouponIdAndUserIdAndStatus(coupon.getId(), userId, CouponRedemption.RedemptionStatus.APPLIED);
            if (userUsed >= coupon.getPerUserLimit()) {
                resp.setMessage("您已達個人使用上限");
                return resp;
            }
        }

        int discount = CouponCalculator.calculateDiscount(
                cartTotal,
                coupon.getDiscountType().name(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount()
        );
        if (discount <= 0) {
            resp.setMessage("優惠券無法折抵金額");
            return resp;
        }

        if (couponRedemptionRepository.findByUserIdAndStatus(userId, CouponRedemption.RedemptionStatus.APPLIED).isPresent()) {
            CouponRedemption couponRedemption = couponRedemptionRepository.findByUserIdAndStatus(userId, CouponRedemption.RedemptionStatus.APPLIED).orElse(null);
            if (couponRedemption == null) {
                throw new IllegalArgumentException("找不到優惠券使用紀錄");
            }
            couponRedemption.setCouponId(coupon.getId());
            couponRedemption.setAmountDiscounted(discount);
            couponRedemptionRepository.save(couponRedemption);
            resp.setDeductionAmount(discount);
            resp.setResult(true);
            resp.setMessage("優惠券更換成功");
            return resp;
        }

        CouponRedemption redemption = new CouponRedemption();
        redemption.setCouponId(coupon.getId());
        redemption.setUserId(userId);
        redemption.setStatus(CouponRedemption.RedemptionStatus.APPLIED);
        redemption.setAmountDiscounted(discount);
        couponRedemptionRepository.save(redemption);

        resp.setDeductionAmount(discount);
        resp.setResult(true);
        resp.setMessage("優惠券使用成功");
        return resp;
    }

    /**
     * 重複建立訂單
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> createOrder(Long userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到購物車");
        }
        Long cartId = cart.getId();
        List<CartItem> items = cartItemRepository.findByCart_Id(cartId);
        if (items == null || items.isEmpty()) return null;
        int total = items.stream()
                .mapToInt(item -> item.getSnapshotPrice() != null ? item.getSnapshotPrice() : 0)
                .sum();
        // 訂單編號 ODyyyyMMddxxxxx
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        long serial = ordersRepository.count() + 1;
        String orderNo = String.format("OD%s%05d", dateStr, serial);

        int pointsDeduction = pointReservationsRepository
                .findByUserIdAndStatus(userId, ReservationStatus.ACTIVE)
                .map(PointReservations::getDeductionAmount)
                .orElse(0);
        int couponDeduction = couponRedemptionRepository
                .findByUserIdAndStatus(userId, CouponRedemption.RedemptionStatus.APPLIED)
                .map(CouponRedemption::getAmountDiscounted)
                .orElse(0);
        int payable = total - pointsDeduction - couponDeduction;


        // 建立訂單
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setCartId(cartId);
        order.setTotalAmount(total);
        order.setDiscountAmount(couponDeduction);
        order.setPointsDeductionAmount(pointsDeduction);
        order.setStatus(OrderStatus.CREATED);
        order.setPayableAmount(payable);
        ordersRepository.save(order);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        // 建立訂單明細

        List<OrderItems> savedItems = new ArrayList<>();
        for (CartItem item : items) {
            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(order);
            orderItem.setBook(item.getBook());
            orderItem.setNameSnapshot(item.getBook().getTitle());
            orderItem.setPriceSnapshot(item.getSnapshotPrice());
            orderItemsRepository.save(orderItem);
            savedItems.add(orderItem);
        }
        String itemName = savedItems.stream()
                .map(OrderItems::getNameSnapshot)
                .collect(Collectors.joining("#"));

        String amount =String.valueOf(payable);

        Map<String, String> orderInfo = new HashMap<>();
        orderInfo.put("tradeNo", orderNo);
        orderInfo.put("amount",amount);
        orderInfo.put("itemName", itemName);

        return orderInfo;

    }


    /**
     *
     * @param tradeNo
     * @param paymentType
     * @param paymentDate
     */

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notify(String tradeNo, String paymentType, LocalDateTime paymentDate) {
        Orders order = ordersRepository.findByOrderNo(tradeNo).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("找不到訂單");
        }
        order.setStatus(OrderStatus.FULFILLED);
        order.setPaymentType(paymentType);
        order.setPaidAt(paymentDate);
        ordersRepository.save(order);

        Long orderId=order.getId();
        Long userId=order.getUserId();
        Integer totalAmount=order.getTotalAmount();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("找不到用戶");
        }
        MembershipTier nowtier = user.getMembershipTier();
        Integer membershipAmout = user.getMembershipWindowTotal();
        MembershipTier tier = fromAmount(membershipAmout +totalAmount);

        user.setMembershipTier(tier);
        user.setMembershipWindowTotal(membershipAmout +totalAmount);
        userRepository.save(user);

        PointRule rule = pointRuleRepository.findTopByOrderByIdDesc().orElse(null);
        if (rule == null) {
            throw new IllegalArgumentException("找不到點數規則");
        }
        int points = CashbackCalculator.calculatePoints(order.getTotalAmount(), nowtier, rule);


        PointLot lotEarned = new PointLot();
        lotEarned.setUserId(userId);
        lotEarned.setSource(PointLot.Source.ORDER);
        lotEarned.setRelatedOrderId(orderId);
        lotEarned.setEarnedPoints(points);
        lotEarned.setExpiresAt(LocalDateTime.now().plusDays(rule.getRollingDays()));
        pointLotRepository.save(lotEarned);

        PointsAccount pointsAccount = pointsAccountRepository.findById(userId)
                .orElseGet(() -> {
            PointsAccount pa = new PointsAccount();
            pa.setUserId(userId);
            pa.setBalance(0);
            return pointsAccountRepository.save(pa);
        });

        Integer balance = pointsAccount.getBalance();

        PointsLedger ledgerEarned = new PointsLedger();
        ledgerEarned.setUserId(userId);
        ledgerEarned.setChangeAmount(points);
        ledgerEarned.setReason(PointsLedger.Reason.PURCHASE_REWARD);
        ledgerEarned.setRelatedOrderId(orderId);
        ledgerEarned.setBalanceAfter(balance+points);
        pointsLedgerRepository.save(ledgerEarned);

        pointsAccount.setBalance(balance + points);
        pointsAccountRepository.save(pointsAccount);

        Integer couponDeduction = order.getDiscountAmount();
        Integer pointsDeduction = order.getPointsDeductionAmount();

        if(couponDeduction>0 && pointsDeduction>0) {
            PointReservations pointReservations = pointReservationsRepository.findByUserIdAndStatus(userId, ReservationStatus.ACTIVE)
                    .orElse(null);
            pointReservations.setStatus(ReservationStatus.COMMITTED);
            pointReservationsRepository.save(pointReservations);
            Integer reservedPts = pointReservations != null ? pointReservations.getReservedPts() : 0;

            PointLot lotUsed = new PointLot();
            lotUsed.setUserId(userId);
            lotUsed.setSource(PointLot.Source.ORDER);
            lotUsed.setRelatedOrderId(orderId);
            lotUsed.setUsedPoints(reservedPts);
            pointLotRepository.save(lotUsed);

            PointsLedger ledgerUsed = new PointsLedger();
            ledgerUsed.setUserId(userId);
            ledgerUsed.setChangeAmount(-reservedPts);
            ledgerUsed.setReason(PointsLedger.Reason.REDEEM);
            ledgerUsed.setRelatedOrderId(orderId);
            ledgerUsed.setBalanceAfter(balance + points - reservedPts);
            pointsLedgerRepository.save(ledgerUsed);

            pointsAccount.setBalance(balance + points - reservedPts);
            pointsAccountRepository.save(pointsAccount);

            CouponRedemption couponRedemption = couponRedemptionRepository.findByUserIdAndStatus(userId, CouponRedemption.RedemptionStatus.APPLIED).orElse(null);
            assert couponRedemption != null;
            couponRedemption.setOrderId(orderId);
            couponRedemption.setRedeemedAt(LocalDateTime.now());
            couponRedemption.setStatus(CouponRedemption.RedemptionStatus.USED);
            couponRedemptionRepository.save(couponRedemption);
            return;
        }
        else if(pointsDeduction>0) {
            PointReservations pointReservations = pointReservationsRepository.findByUserIdAndStatus(userId, ReservationStatus.ACTIVE)
                    .orElse(null);
            pointReservations.setStatus(ReservationStatus.COMMITTED);
            pointReservationsRepository.save(pointReservations);
            Integer reservedPts = pointReservations != null ? pointReservations.getReservedPts() : 0;

            PointLot lotUsed = new PointLot();
            lotUsed.setUserId(userId);
            lotUsed.setSource(PointLot.Source.ORDER);
            lotUsed.setRelatedOrderId(orderId);
            lotUsed.setUsedPoints(reservedPts);
            pointLotRepository.save(lotUsed);

            PointsLedger ledgerUsed = new PointsLedger();
            ledgerUsed.setUserId(userId);
            ledgerUsed.setChangeAmount(-reservedPts);
            ledgerUsed.setReason(PointsLedger.Reason.REDEEM);
            ledgerUsed.setRelatedOrderId(orderId);
            ledgerUsed.setBalanceAfter(balance + points - reservedPts);
            pointsLedgerRepository.save(ledgerUsed);

            pointsAccount.setBalance(balance + points - reservedPts);
            pointsAccountRepository.save(pointsAccount);
            return;
        }
        else if(couponDeduction>0) {
            CouponRedemption couponRedemption = couponRedemptionRepository.findByUserIdAndStatus(userId, CouponRedemption.RedemptionStatus.APPLIED).orElse(null);
            assert couponRedemption != null;
            couponRedemption.setOrderId(orderId);
            couponRedemption.setRedeemedAt(LocalDateTime.now());
            couponRedemption.setStatus(CouponRedemption.RedemptionStatus.USED);
            couponRedemptionRepository.save(couponRedemption);
            return;
        }
        else {
            return;
        }
    }


}
