package com.connectsphere.search_service.dto;

import lombok.*;

import java.util.List;

// Response wrapper carrying a list of matched post IDs.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostIdListResponseDto {

    private List<Integer> postIds;
    private long          total;
}
