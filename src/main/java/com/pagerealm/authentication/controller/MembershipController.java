package com.pagerealm.authentication.controller;

import com.pagerealm.authentication.dto.MembershipStatusDTO;
import com.pagerealm.authentication.entity.User;
import com.pagerealm.authentication.repository.UserRepository;
import com.pagerealm.authentication.security.service.UserDetailsImpl;
import com.pagerealm.authentication.service.MembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final UserRepository userRepository;

    public MembershipController(MembershipService membershipService, UserRepository userRepository) {
        this.membershipService = membershipService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<MembershipStatusDTO> status(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(membershipService.getStatus(userDetails.getId()));
    }

    // 給他組整合或本地測試用：紀錄一筆消費
    @PostMapping("/purchase")
    public ResponseEntity<Void> recordPurchase(Principal principal,
                                               @RequestParam("amount") Integer amount) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        membershipService.recordPurchase(user.getUserId(), amount, LocalDateTime.now());
        return ResponseEntity.ok().build();
    }
}
