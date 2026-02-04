package com.ecommerce.user.exception;

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

  @Schema(description = "Map of field names to their validation error messages", example = "{\"email\": \"must be a valid email address\", \"password\": \"size must be between 6 and 100\"}")
  private final Map<String, String> fieldErrors;

  public ValidationErrorResponse(int status, String message, String code,
      Map<String, String> fieldErrors) {
    super(status, message, code, System.currentTimeMillis());
    this.fieldErrors = fieldErrors;
  }
}
