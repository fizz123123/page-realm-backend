package com.shop.shopping.admin_log.adminshop.shopcontroller;

import com.shop.shopping.admin_log.adminshop.shopdto.OrderDtos;
import com.shop.shopping.shoppingcart.entity.OrderItems;
import com.shop.shopping.shoppingcart.entity.OrderStatus;
import com.shop.shopping.shoppingcart.entity.Orders;
import com.shop.shopping.shoppingcart.repository.OrderItemsRepository;
import com.shop.shopping.shoppingcart.repository.OrdersRepository;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminOrdersController {

    private final OrdersRepository ordersRepository;
    // 新增：熱門書籍統計需要
    private final OrderItemsRepository orderItemsRepository;

    public AdminOrdersController(OrdersRepository ordersRepository, OrderItemsRepository orderItemsRepository) {
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    @GetMapping
    public Page<OrderDtos.OrderListItem> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        // 解析排序
        Sort sortObj;
        if (sort.contains(",")) {
            String[] s = sort.split(",");
            sortObj = Sort.by("desc".equalsIgnoreCase(s[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, s[0]);
        } else {
            sortObj = Sort.by(Sort.Direction.DESC, sort);
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // 動態條件（以 conjunction 作為初始條件，避免使用已棄用的 Specification.where）
        Specification<Orders> spec = (root, q, cb) -> cb.conjunction();
        if (orderNo != null && !orderNo.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("orderNo"), orderNo.trim()));
        }
        if (userId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus st = OrderStatus.valueOf(status.trim());
                spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), st));
            } catch (IllegalArgumentException ignored) {}
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            LocalDateTime from = parseDateTimeStart(dateFrom.trim());
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (dateTo != null && !dateTo.isBlank()) {
            LocalDateTime to = parseDateTimeEnd(dateTo.trim());
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        Page<Orders> pageData = ordersRepository.findAll(spec, pageable);
        List<OrderDtos.OrderListItem> list = pageData.getContent().stream().map(this::toListItem).collect(Collectors.toList());
        return new PageImpl<>(list, pageable, pageData.getTotalElements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDtos.OrderDetail> detail(@PathVariable Long id) {
        Orders o = ordersRepository.findWithItemsById(id).orElse(null);
        if (o == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDetail(o));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDtos.OrderDetail> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req) {
        Orders o = ordersRepository.findWithItemsById(id).orElse(null);
        if (o == null) return ResponseEntity.notFound().build();
        if (req.status != null && !req.status.isBlank()) {
            try {
                o.setStatus(OrderStatus.valueOf(req.status.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (req.paymentType != null && !req.paymentType.isBlank()) {
            o.setPaymentType(req.paymentType.trim());
        }
        if (Objects.equals(o.getStatus(), OrderStatus.PAID) || Objects.equals(o.getStatus(), OrderStatus.FULFILLED)) {
            if (o.getPaidAt() == null) o.setPaidAt(LocalDateTime.now());
        }
        ordersRepository.save(o);
        return ResponseEntity.ok(toDetail(o));
    }

    // 新增：訂單狀態分佈統計端點
    @GetMapping("/stats")
    public ResponseEntity<OrderStatusStatsResponse> stats(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (dateFrom != null && !dateFrom.isBlank()) from = parseDateTimeStart(dateFrom.trim());
        if (dateTo != null && !dateTo.isBlank()) to = parseDateTimeEnd(dateTo.trim());

        List<OrdersRepository.StatusCount> rows = ordersRepository.countByStatusBetween(from, to);
        Map<OrderStatus, Long> map = new EnumMap<>(OrderStatus.class);
        for (OrdersRepository.StatusCount r : rows) {
            map.put(r.getStatus(), r.getCnt());
        }
        // 固定輸出順序，與前端圖例一致
        OrderStatus[] order = new OrderStatus[]{OrderStatus.FULFILLED, OrderStatus.PAID, OrderStatus.PAYING, OrderStatus.CREATED, OrderStatus.FAILED};
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        long total = 0L;
        for (OrderStatus st : order) {
            labels.add(st.name());
            long v = map.getOrDefault(st, 0L);
            values.add(v);
            total += v;
        }
        OrderStatusStatsResponse resp = new OrderStatusStatsResponse(labels, values, total);
        return ResponseEntity.ok(resp);
    }

    // 新增：最近 N 天每日銷售額（以 payableAmount，僅計 PAID/FULFILLED）
    @GetMapping("/sales-trend")
    public ResponseEntity<TrendResponse> salesTrend(@RequestParam(defaultValue = "30") @Min(1) int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.PAID, OrderStatus.FULFILLED);

        List<OrdersRepository.DailySumRow> rows = ordersRepository.sumPayableByDay(statuses, from, to);
        Map<String, Long> map = new HashMap<>();
        for (OrdersRepository.DailySumRow r : rows) {
            String key = String.format("%04d-%02d-%02d", r.getY(), r.getM(), r.getD());
            map.put(key, r.getTotal() == null ? 0L : r.getTotal());
        }
        DateTimeFormatter labFmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            String key = d.toString();
            labels.add(d.format(labFmt));
            values.add(map.getOrDefault(key, 0L));
        }
        return ResponseEntity.ok(new TrendResponse(labels, values));
    }

    // 新增：熱門書籍 Top N（依訂單品項數量，僅計 PAID/FULFILLED，近 N 天）
    @GetMapping("/top-books")
    public ResponseEntity<TopResponse> topBooks(
            @RequestParam(defaultValue = "30") @Min(1) int days,
            @RequestParam(defaultValue = "5") @Min(1) int limit
    ) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.PAID, OrderStatus.FULFILLED);

        List<OrderItemsRepository.TopBookRow> rows = orderItemsRepository.topBooksByCount(statuses, from, to);
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < limit; i++) {
            labels.add(rows.get(i).getName());
            values.add(rows.get(i).getQty());
        }
        return ResponseEntity.ok(new TopResponse(labels, values));
    }

    private OrderDtos.OrderListItem toListItem(Orders o) {
        return OrderDtos.OrderListItem.builder()
                .id(o.getId())
                .orderNo(o.getOrderNo())
                .userId(o.getUserId())
                .totalAmount(o.getTotalAmount())
                .discountAmount(o.getDiscountAmount())
                .pointsDeductionAmount(o.getPointsDeductionAmount())
                .payableAmount(o.getPayableAmount())
                .status(o.getStatus())
                .paymentType(o.getPaymentType())
                .createdAt(o.getCreatedAt())
                .paidAt(o.getPaidAt())
                .build();
    }

    private OrderDtos.OrderDetail toDetail(Orders o) {
        List<OrderDtos.OrderItemDetail> items = new ArrayList<>();
        for (OrderItems it : o.getItems()) {
            items.add(OrderDtos.OrderItemDetail.builder()
                    .id(it.getId())
                    .bookId(it.getBook() != null ? it.getBook().getId() : null)
                    .name(it.getNameSnapshot())
                    .price(it.getPriceSnapshot())
                    .build());
        }
        return OrderDtos.OrderDetail.builder()
                .id(o.getId())
                .orderNo(o.getOrderNo())
                .userId(o.getUserId())
                .totalAmount(o.getTotalAmount())
                .discountAmount(o.getDiscountAmount())
                .pointsDeductionAmount(o.getPointsDeductionAmount())
                .payableAmount(o.getPayableAmount())
                .status(o.getStatus())
                .paymentType(o.getPaymentType())
                .createdAt(o.getCreatedAt())
                .paidAt(o.getPaidAt())
                .items(items)
                .build();
    }

    private LocalDateTime parseDateTimeStart(String s) {
        // 支援 yyyy-MM-dd 或 yyyy-MM-ddTHH:mm:ss
        if (s.length() <= 10) {
            LocalDate d = LocalDate.parse(s);
            return d.atStartOfDay();
        }
        return LocalDateTime.parse(s);
    }
    private LocalDateTime parseDateTimeEnd(String s) {
        if (s.length() <= 10) {
            LocalDate d = LocalDate.parse(s);
            return d.atTime(LocalTime.MAX);
        }
        return LocalDateTime.parse(s);
    }

    public static class UpdateStatusRequest {
        public String status;
        public String paymentType;
    }

    // 回應 DTO：訂單狀態分佈
    public record OrderStatusStatsResponse(List<String> labels, List<Long> values, long total) {}
    // 新增：統一回應格式（labels/values）
    public record TrendResponse(List<String> labels, List<Long> values) {}
    public record TopResponse(List<String> labels, List<Long> values) {}
}
