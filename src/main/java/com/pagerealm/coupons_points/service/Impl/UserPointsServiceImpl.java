package com.pagerealm.coupons_points.service.Impl;

import com.pagerealm.coupons_points.dto.points.UserPointsResponse;
import com.pagerealm.coupons_points.entity.PointLot;
import com.pagerealm.coupons_points.repository.PointLotRepository;
import com.pagerealm.coupons_points.service.UserPointsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserPointsServiceImpl implements UserPointsService {
    private PointLotRepository pointLotRepository;

    public UserPointsServiceImpl(PointLotRepository pointLotRepository) {
        this.pointLotRepository = pointLotRepository;
    }

    @Override
    public List<UserPointsResponse> getPoints(Long userId) {
        List<PointLot> pointLots = pointLotRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<UserPointsResponse> response = new ArrayList<>();

        for (PointLot pointLot : pointLots) {
            UserPointsResponse userPointsResponse = new UserPointsResponse();
            userPointsResponse.setCreateAt(pointLot.getCreatedAt().toLocalDate());
            userPointsResponse.setEarnedPoints(pointLot.getEarnedPoints());
            userPointsResponse.setUsedPoints(pointLot.getUsedPoints());


            if(pointLot.getEarnedPoints() == 0){
                userPointsResponse.setReason("點數折抵");
                userPointsResponse.setExpiredAt(null);
                response.add(userPointsResponse);

            }else {
                userPointsResponse.setReason("消費獲得");
                userPointsResponse.setExpiredAt(pointLot.getExpiresAt().toLocalDate());
                response.add(userPointsResponse);
            }
        }
        return response;
    }
}
