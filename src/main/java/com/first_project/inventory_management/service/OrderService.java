package com.first_project.inventory_management.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.first_project.inventory_management.repository.*;

import jakarta.persistence.EntityNotFoundException;

import com.first_project.inventory_management.entity.*;
import com.first_project.inventory_management.enums.OrderStatus;
import com.first_project.inventory_management.dto.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request){

        // Step 1 — Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
        
        // Step 2 — Validate all products and check stock
        // We do ALL validation before touching any stock
        // This prevents partial deductions if one product fails midway
        for(OrderItemRequestDTO itemRequest : request.getItems()){
            Product product = productRepository.findById(itemRequest.getProductId())
                                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + itemRequest.getProductId()));;

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                    "Insufficient stock for product '" + product.getName() +
                    "'. Available: " + product.getStockQuantity() +
                    ". Requested: " + itemRequest.getQuantity());
            }
        }

        // Step 3 — Build the Order first (without items)
        Order order = Order.builder()
                        .customer(customer)
                        .status(OrderStatus.PENDING)
                        .orderDate(LocalDateTime.now())
                        .totalAmount(0.0)
                        .build();

        Order savedOrder = orderRepository.save(order);

        // Step 4 — Process each item: deduct stock, create OrderItem, accumulate total
        double totalAmount = 0.0;

        for(OrderItemRequestDTO itemRequest : request.getItems()){
            Product product = productRepository.findById(itemRequest.getProductId()).get();

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            // Snapshot the price at time of purchase
            double priceAtPurchase = product.getPrice();
            totalAmount += priceAtPurchase * itemRequest.getQuantity();

            // Create OrderItem
            OrderItem orderItem = OrderItem.builder()
                                        .order(savedOrder)
                                        .product(product)
                                        .quantity(itemRequest.getQuantity())
                                        .priceAtPurchase(priceAtPurchase)
                                        .build();
            orderItemRepository.save(orderItem);
        }

        // Step 5 — Update total amount on the order
        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId){

        // Step 1 — Find the order
        Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Step 2 — Check it's not already cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order is already cancelled");                
        }

        // Step 3 — Restore stock for each item
        for(OrderItem item : order.getItems()){
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // Step 4 — Update order status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return mapToResponse(order);
    }

    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        return mapToResponse(order);
    }

    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                            .stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException("Customer not found with ID: " + customerId);
        }

        return orderRepository.findByCustomer_Id(customerId)
                            .stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
    }

    private OrderResponseDTO mapToResponse(Order order){
        List<OrderItemResponseDTO> itemDTOs = order.getItems() == null ? List.of() :
                    order.getItems().stream()
                            .map(item -> OrderItemResponseDTO.builder()
                                                    .productId(item.getProduct().getId())
                                                    .productName(item.getProduct().getName())
                                                    .quantity(item.getQuantity())
                                                    .priceAtPurchase(item.getPriceAtPurchase())
                                                    .subtotal(item.getPriceAtPurchase()*item.getQuantity())
                                                    .build())
                            .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                        .id(order.getId())
                        .customerName(order.getCustomer().getName())
                        .status(order.getStatus())
                        .totalAmount(order.getTotalAmount())
                        .ordeDate(order.getOrderDate())
                        .items(itemDTOs)
                        .build();
    }
}
