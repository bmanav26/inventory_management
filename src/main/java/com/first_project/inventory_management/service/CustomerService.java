package com.first_project.inventory_management.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.first_project.inventory_management.dto.CustomerRequestDTO;
import com.first_project.inventory_management.dto.CustomerResponseDTO;
import com.first_project.inventory_management.entity.Customer;
import com.first_project.inventory_management.repository.CustomerRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerResponseDTO createCustomer(CustomerRequestDTO request) {
        if(customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Customer with email: " + request.getEmail() + "already exists");
        }
        Customer customer = Customer.builder()
                                    .name(request.getName())
                                    .email(request.getEmail())
                                    .phone(request.getPhone())
                                    .address(request.getAddress())
                                    .build();
        return mapToResponse(customerRepository.save(customer));
    }
    
    public List<CustomerResponseDTO> getAllCustomers() {
        return customerRepository.findAll()
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
    }

    public CustomerResponseDTO getCustomerById(Long id){
        Customer customer = customerRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));

        return mapToResponse(customer);
    }

    public CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO request) {
        Customer customer = customerRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());

        return mapToResponse(customerRepository.save(customer));
    }

    public void deleteCustomer(Long id) {
        if(!customerRepository.existsById(id)){
            throw new EntityNotFoundException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }

    private CustomerResponseDTO mapToResponse(Customer customer){
        return CustomerResponseDTO.builder()
                                .id(customer.getId())
                                .name(customer.getName())
                                .email(customer.getEmail())
                                .phone(customer.getPhone())
                                .address(customer.getAddress())
                                .build();
    }
}
