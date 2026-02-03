package com.ecommerce.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Test", description = "Endpoints for testing notification delivery")
public class NotificationTestController {

  @Operation(summary = "Send a test notification", description = "Manually triggers a notification log (for testing purposes)")
  @PostMapping("/test")
  public ResponseEntity<String> sendTestNotification(@RequestBody TestNotificationRequest request) {
    log.info("Received test notification request: {}", request);
    return ResponseEntity.ok("Notification logged successfully for: " + request.email());
  }

  public record TestNotificationRequest(String email, String message) {
  }
}
