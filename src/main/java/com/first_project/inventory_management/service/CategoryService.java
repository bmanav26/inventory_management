package com.first_project.inventory_management.service;

import com.first_project.inventory_management.dto.CategoryRequestDTO;
import com.first_project.inventory_management.dto.CategoryResponseDTO;
import com.first_project.inventory_management.entity.Category;
import com.first_project.inventory_management.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    
    public CategoryResponseDTO createCategory(CategoryRequestDTO request){
        if (categoryRepository.existsByName(request.getName())){
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }
        Category category = Category.builder()
                                    .name(request.getName())
                                    .description(request.getDescription())
                                    .build();
        Category saved =  categoryRepository.save(category);
        return mapToResponse(saved);
    }

    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll()
                                 .stream()
                                 .map(this::mapToResponse)
                                 .collect(Collectors.toList());
    }

    public CategoryResponseDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                                              .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id) );
        return mapToResponse(category);
    }
                                            
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                                              .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id) );
        category.setName(request.getName());
        category.setDescription((request.getDescription()));
        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);        
    }

    public void deleteCategory(Long id){
        if(!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponseDTO mapToResponse(Category category) {
        return CategoryResponseDTO.builder()
                                  .id(category.getId())
                                  .name(category.getName())
                                  .description(category.getDescription())
                                  .build();
    }
}
