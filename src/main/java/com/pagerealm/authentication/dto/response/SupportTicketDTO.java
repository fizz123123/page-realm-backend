package com.pagerealm.authentication.dto.response;

import com.pagerealm.authentication.entity.AppTicketCategory;
import com.pagerealm.authentication.entity.AppTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SupportTicketDTO {
    private Long id;
    private String subject;
    private AppTicketCategory category;
    private AppTicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageAt;
}

