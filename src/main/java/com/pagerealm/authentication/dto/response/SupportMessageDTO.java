package com.pagerealm.authentication.dto.response;

import com.pagerealm.authentication.entity.AppSenderType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SupportMessageDTO {
    private Long id;
    private AppSenderType sender;
    private String content;
    private LocalDateTime createdAt;
}

