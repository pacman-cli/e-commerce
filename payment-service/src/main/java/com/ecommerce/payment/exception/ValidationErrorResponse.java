package com.ecommerce.payment.exception;

import java.util.Map;

import com.ecommerce.shared.dto.ErrorResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Response DTO for validation errors.
 * Extends ErrorResponse to include field-level error details.
 */
@Getter
@Schema(description = "Response returned when request validation fails")
public class ValidationErrorResponse extends ErrorResponse {

  @Schema(description = "Map of field names to their validation error messages", example = "{\"amount\": \"must be greater than 0\", \"paymentMethod\": \"must not be empty\"}")
  private final Map<String, String> fieldErrors;

  public ValidationErrorResponse(int status, String message, String error,
      Map<String, String> fieldErrors) {
    super(status, message, error, System.currentTimeMillis());
    this.fieldErrors = fieldErrors;
  }
}
