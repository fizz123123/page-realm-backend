package com.pagerealm.authentication.dto.request;

import com.pagerealm.authentication.entity.AppTicketStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminReplyRequest {
    @NotBlank
    private String content;
    private AppTicketStatus nextStatus; // 可選：預設自動改為 IN_PROGRESS
}

