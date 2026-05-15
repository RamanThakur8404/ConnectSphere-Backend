package com.connectsphere.search_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

// Full hashtag representation returned by the API.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HashtagResponseDto {

    private Integer hashtagId;
    private String  tag;
    private Integer postCount;
    private LocalDateTime lastUsedAt;
}
