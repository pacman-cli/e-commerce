package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
  private UUID id;
  private UUID productId;
  private Integer quantity;
  private BigDecimal price;
  private BigDecimal subtotal;
}
