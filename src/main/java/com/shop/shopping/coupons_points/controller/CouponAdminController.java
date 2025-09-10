package com.shop.shopping.coupons_points.controller;

import com.shop.shopping.coupons_points.dto.coupon.CouponDtos;
import com.shop.shopping.coupons_points.entity.Coupon;
import com.shop.shopping.coupons_points.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponService couponService;

    @PostMapping
    public CouponDtos.CouponResponse create(@RequestBody @Valid CouponDtos.CreateRequest req) {
        return couponService.create(req);
    }

    @PutMapping("/{id}")
    public CouponDtos.CouponResponse update(@PathVariable Long id, @RequestBody @Valid CouponDtos.UpdateRequest req) {
        return couponService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        couponService.delete(id);
    }

    @GetMapping
    public Page<CouponDtos.CouponResponse> list(@RequestParam(value = "status", required = false) Coupon.CouponStatus status,
                                                Pageable pageable) {
        return couponService.listByStatus(status, pageable);
    }

    @GetMapping("/{id}")
    public CouponDtos.CouponResponse getOne(@PathVariable Long id) {
        return couponService.getById(id);
    }

    @PatchMapping("/{id}/status")
    public CouponDtos.CouponResponse changeStatus(@PathVariable Long id, @RequestParam("status") Coupon.CouponStatus status) {
        return couponService.changeStatus(id, status);
    }
}
