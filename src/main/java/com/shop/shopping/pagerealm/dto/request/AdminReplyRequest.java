package com.shop.shopping.pagerealm.dto.request;

import com.shop.shopping.pagerealm.entity.AppTicketStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminReplyRequest {
    @NotBlank
    private String content;
    private AppTicketStatus nextStatus; // 可選：預設自動改為 IN_PROGRESS
}

