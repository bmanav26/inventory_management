package com.first_project.inventory_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String sku;
    private Double price;
    private Integer stockQuantity;
    private Integer reorderLevel;
    private Boolean lowStock;
    private String categoryName;
    private String supplierName;
}
