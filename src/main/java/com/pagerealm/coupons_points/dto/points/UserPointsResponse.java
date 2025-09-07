package com.pagerealm.coupons_points.dto.points;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class UserPointsResponse {
    LocalDate createAt;
    String reason;
    Integer earnedPoints;
    Integer usedPoints;
    LocalDate expiredAt;
}
