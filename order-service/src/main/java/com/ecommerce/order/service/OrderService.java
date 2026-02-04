package com.ecommerce.order.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.OrderStatus;

public interface OrderService {
  CompletableFuture<OrderResponse> createOrder(UUID userId, String userEmail, CreateOrderRequest request);

  CompletableFuture<OrderResponse> getOrderById(UUID orderId);

  Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable);

  void updateOrderStatus(UUID orderId, OrderStatus status);
}
