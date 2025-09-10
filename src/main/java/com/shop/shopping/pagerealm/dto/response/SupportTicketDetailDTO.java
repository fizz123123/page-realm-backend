package com.shop.shopping.pagerealm.dto.response;

import com.shop.shopping.pagerealm.entity.AppTicketCategory;
import com.shop.shopping.pagerealm.entity.AppTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class SupportTicketDetailDTO {
    private Long id;
    private String contactName;
    private String contactEmail;
    private String subject;
    private AppTicketCategory category;
    private AppTicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageAt;
    private List<SupportMessageDTO> messages;
}

