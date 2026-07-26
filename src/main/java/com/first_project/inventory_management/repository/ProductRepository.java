package com.first_project.inventory_management.repository;

import com.first_project.inventory_management.entity.Product;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Boolean existsBySku(String sku);
    List<Product> findByCategory_Id(Long categoryId);
    List<Product> findBySupplier_Id(Long supplierId);
    List<Product> findByStockQuantityLessThanEqual(Integer threshold);
}
