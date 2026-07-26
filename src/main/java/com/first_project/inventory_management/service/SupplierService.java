package com.first_project.inventory_management.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.first_project.inventory_management.dto.SupplierRequestDTO;
import com.first_project.inventory_management.dto.SupplierResponseDTO;
import com.first_project.inventory_management.entity.Supplier;
import com.first_project.inventory_management.repository.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {
    
    private final SupplierRepository supplierRepository;

    public SupplierResponseDTO createSupplier(SupplierRequestDTO request){
        if(supplierRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Supplier with email " + request.getEmail() + " already exists");
        }

        Supplier supplier = Supplier.builder()
                            .name(request.getName())
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .address(request.getAddress())
                            .build();

        return mapToResponse(supplierRepository.save(supplier));
    }

    public List<SupplierResponseDTO> getAllSuppliers() {
        return supplierRepository.findAll()
                                 .stream()
                                 .map(this::mapToResponse)
                                 .collect(Collectors.toList());
    }

    public SupplierResponseDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Supplier not found with ID: " + id));
        return mapToResponse(supplier);
    }

    public SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO request){
        Supplier supplier = supplierRepository.findById(id)
                            .orElseThrow(()-> new EntityNotFoundException("Supplier Not found with ID: " + id));

        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        return mapToResponse(supplierRepository.save(supplier));
    }

    public void deleteSupplier(Long id){
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("Supplier not found by ID: " + id);
        }
        supplierRepository.deleteById(id);
    }

    private SupplierResponseDTO mapToResponse(Supplier supplier){
        return SupplierResponseDTO.builder()
                                .id(supplier.getId())
                                .name(supplier.getName())
                                .email(supplier.getEmail())
                                .phone(supplier.getPhone())
                                .address(supplier.getAddress())
                                .build();
    }
}
