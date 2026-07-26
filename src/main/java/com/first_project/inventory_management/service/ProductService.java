package com.first_project.inventory_management.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.first_project.inventory_management.dto.ProductRequestDTO;
import com.first_project.inventory_management.dto.ProductResponseDTO;
import com.first_project.inventory_management.entity.Category;
import com.first_project.inventory_management.entity.Product;
import com.first_project.inventory_management.entity.Supplier;
import com.first_project.inventory_management.repository.CategoryRepository;
import com.first_project.inventory_management.repository.ProductRepository;
import com.first_project.inventory_management.repository.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductResponseDTO createProduct(ProductRequestDTO request){
        if (request.getSku() != null && productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product with SKU '"+request.getSku()+"' already exists");
        }

         Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));


        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                            .orElseThrow(() -> new EntityNotFoundException("Supplier not found with ID: " + request.getSupplierId()));

        Product product = Product.builder()
                                 .name(request.getName())
                                 .sku(request.getSku())
                                 .price(request.getPrice())
                                 .stockQuantity(request.getStockQuantity())
                                 .reorderLevel(request.getReorderLevel()!= null? request.getReorderLevel() : 10)
                                 .category(category)
                                 .supplier(supplier)
                                 .build();

        return mapToResponse(productRepository.save(product));
    }

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
    }

    public ProductResponseDTO getProductById(Long id){
        Product product = productRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Product not found for ID: " + id));
        return mapToResponse(product);
    }

    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request){
        Product product = productRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: "+id));

        Category category = categoryRepository.findById(request.getCategoryId())
                            .orElseThrow(() -> new EntityNotFoundException("Category not found for ID: "+request.getCategoryId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                            .orElseThrow(() -> new EntityNotFoundException("Supplier not found with ID: " + request.getSupplierId()));

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());;
        product.setReorderLevel(request.getReorderLevel() != null? request.getReorderLevel():10);
        product.setCategory(category);
        product.setSupplier(supplier);

        return mapToResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id){
        if(!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    public List<ProductResponseDTO> getLowStockProducts()  {
        return productRepository.findAll()
                                .stream()
                                .filter(p -> p.getStockQuantity() <= p.getReorderLevel())
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
    }

    private ProductResponseDTO mapToResponse(Product product){
        return ProductResponseDTO.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .sku(product.getSku())
                                .price(product.getPrice())
                                .stockQuantity(product.getStockQuantity())
                                .reorderLevel(product.getReorderLevel())
                                .lowStock(product.getStockQuantity() <=  product.getReorderLevel())
                                .categoryName(product.getCategory().getName())
                                .supplierName(product.getSupplier().getName())
                                .build();
    }
}
