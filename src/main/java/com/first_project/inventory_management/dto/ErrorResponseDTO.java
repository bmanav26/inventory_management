package com.first_project.inventory_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
    public class ErrorResponseDTO {
        private int status;
        private String message;
        private LocalDateTime timestamp;
}
