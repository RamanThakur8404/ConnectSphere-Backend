package com.connectsphere.message_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequestDTO {
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String content;
}
