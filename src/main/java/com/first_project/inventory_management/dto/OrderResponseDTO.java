package com.first_project.inventory_management.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.first_project.inventory_management.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String customerName;
    private OrderStatus status;
    private Double totalAmount;
    private LocalDateTime ordeDate;
    private List<OrderItemResponseDTO> items;
}
