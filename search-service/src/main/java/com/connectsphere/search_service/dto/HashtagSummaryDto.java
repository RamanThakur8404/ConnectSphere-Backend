package com.connectsphere.search_service.dto;

import lombok.*;

import java.time.LocalDateTime;

// Lightweight trending hashtag entry.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HashtagSummaryDto {

    private String  tag;
    private Integer postCount;
    private LocalDateTime lastUsedAt;
}
