package com.ra.freshChickenAPI.service;

import com.ra.freshChickenAPI.controller.OrderActivityController;
import com.ra.freshChickenAPI.dto.CreateOrderRequest;
import com.ra.freshChickenAPI.dto.OrderActivityMessage;
import com.ra.freshChickenAPI.dto.OrderItemRequest;
import com.ra.freshChickenAPI.entity.*;
import com.ra.freshChickenAPI.repository.AddressRepository;
import com.ra.freshChickenAPI.repository.CustomerRepository;
import com.ra.freshChickenAPI.repository.OrderRepository;
import com.ra.freshChickenAPI.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderActivityController orderActivityController;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Page<Order> getAllOrders(Pageable pageable, OrderStatus status, LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (status != null && dateFrom != null && dateTo != null) {
            return orderRepository.findByStatusAndOrderDateBetween(status, dateFrom, dateTo, pageable);
        } else if (status != null) {
            return orderRepository.findByStatus(status, pageable);
        } else if (dateFrom != null && dateTo != null) {
            return orderRepository.findByOrderDateBetween(dateFrom, dateTo, pageable);
        } else {
            return orderRepository.findAll(pageable);
        }
    }

    public List<Order> getOrdersByStatuses(List<OrderStatus> statuses) {
        return orderRepository.findByStatusIn(statuses);
    }
    
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }
    
    public List<Order> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    @Transactional
    public Order createOrderFromRequest(CreateOrderRequest request) {
        // Validate customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + request.getCustomerId()));
        
        // Validate address
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + request.getAddressId()));
        
        // Verify address belongs to customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Address does not belong to this customer");
        }
        
        // Create order
        Order order = new Order();
        order.setCustomer(customer);
        order.setAddress(address);
        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        
        // Parse and set delivery date from deliverySlot if needed
        // Format: "2024-11-05 - 10:00 AM - 12:00 PM"
        if (request.getDeliverySlot() != null && !request.getDeliverySlot().isEmpty()) {
            // You can parse this and set deliveryDate if needed
            // For now, we'll store it in specialInstructions or create a new field
        }
        
        // Create order items and calculate total
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemRequest.getProductId()));
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            orderItem.setSubtotal(subtotal);
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }
        
        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // Send real-time notification for new order
        OrderActivityMessage message = new OrderActivityMessage(
                "ORDER_CREATED",
                savedOrder.getId(),
                customer.getName(),
                savedOrder.getStatus().toString(),
                null,
                LocalDateTime.now(),
                "New order #" + savedOrder.getId() + " placed by " + customer.getName()
        );
        orderActivityController.sendOrderActivityNotification(message);

        return savedOrder;
    }
    
    @Transactional
    public Order createOrder(Order order) {
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING);
        }
        
        // Calculate total from order items
        BigDecimal total = order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setTotalAmount(total);
        
        // Set order reference for each order item
        order.getOrderItems().forEach(item -> {
            item.setOrder(order);
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setSubtotal(subtotal);
        });
        
        return orderRepository.save(order);
    }
    
    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        String previousStatus = order.getStatus().toString();

        order.setStatus(status);

        if (status == OrderStatus.DELIVERED) {
            order.setDeliveryDate(LocalDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);

        // Send real-time notification for status update
        OrderActivityMessage message = new OrderActivityMessage(
                "ORDER_STATUS_UPDATED",
                updatedOrder.getId(),
                updatedOrder.getCustomer().getName(),
                status.toString(),
                previousStatus,
                LocalDateTime.now(),
                "Order #" + updatedOrder.getId() + " status changed from " + previousStatus + " to " + status.toString()
        );
        orderActivityController.sendOrderActivityNotification(message);

        return updatedOrder;
    }
    
    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}