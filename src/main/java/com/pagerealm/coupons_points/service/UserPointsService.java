package com.pagerealm.coupons_points.service;

import com.pagerealm.coupons_points.dto.points.UserPointsResponse;

import java.util.List;

public interface UserPointsService {
    /**
     * 用戶歷史點數
     */
    List<UserPointsResponse> getPoints(Long userId);
}
