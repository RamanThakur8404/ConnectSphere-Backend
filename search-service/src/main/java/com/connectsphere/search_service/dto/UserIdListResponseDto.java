package com.connectsphere.search_service.dto;

import lombok.*;

import java.util.List;

// Response wrapper carrying a list of matched user IDs.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdListResponseDto {

    private List<Integer> userIds;
    private long          total;
}
