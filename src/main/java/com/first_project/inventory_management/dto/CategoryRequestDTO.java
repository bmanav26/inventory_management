package com.first_project.inventory_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequestDTO {
    
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;
}
