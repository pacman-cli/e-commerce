package com.ecommerce.order.exception;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO for validation errors.
 */
@Data
@AllArgsConstructor
@Schema(description = "Response returned when request validation fails")
public class ValidationErrorResponse {

  @Schema(description = "Timestamp of the error", example = "2023-10-20T14:30:00")
  private LocalDateTime timestamp;

  @Schema(description = "HTTP Status code", example = "400")
  private int status;

  @Schema(description = "Error code", example = "VALIDATION_FAILED")
  private String code;

  @Schema(description = "Error message", example = "Request validation failed")
  private String message;

  @Schema(description = "Map of field names to their validation error messages", example = "{\"productId\": \"must not be null\", \"quantity\": \"must be greater than 0\"}")
  private Map<String, String> fieldErrors;
}
