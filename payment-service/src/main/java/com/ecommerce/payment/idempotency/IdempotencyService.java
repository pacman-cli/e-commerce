package com.ecommerce.payment.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for handling idempotent operations.
 * 
 * Ensures that requests with the same idempotency key produce the same result,
 * preventing duplicate processing of the same operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final IdempotentRequestRepository repository;
    private final ObjectMapper objectMapper;
    
    /**
     * Check if a request with the given idempotency key has already been processed.
     * 
     * @param idempotencyKey The idempotency key from the request header
     * @return Optional containing the previous response if found and not expired
     */
    @Transactional(readOnly = true)
    public Optional<IdempotentRequest> getPreviousResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        
        return repository.findByIdempotencyKey(idempotencyKey)
            .filter(req -> !req.isExpired());
    }
    
    /**
     * Store the response for an idempotent request.
     * 
     * @param idempotencyKey The idempotency key
     * @param requestBody The request body (for hash verification)
     * @param requestType The type of request (e.g., "ProcessPayment")
     * @param response The response object to store
     * @param statusCode The HTTP status code of the response
     * @param ttlHours How long to keep the idempotency record (default: 24 hours)
     */
    @Transactional
    public void storeResponse(
            String idempotencyKey,
            String requestBody,
            String requestType,
            Object response,
            int statusCode,
            int ttlHours) {
        
        try {
            String requestHash = computeHash(requestBody);
            String responseJson = objectMapper.writeValueAsString(response);
            
            IdempotentRequest request = IdempotentRequest.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .requestType(requestType)
                .responsePayload(responseJson)
                .responseStatusCode(statusCode)
                .expiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS))
                .build();
            
            repository.save(request);
            
            log.debug("Stored idempotent response for key: {}, type: {}", 
                idempotencyKey, requestType);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency storage", e);
        }
    }
    
    /**
     * Store response with default 24-hour TTL.
     */
    @Transactional
    public void storeResponse(
            String idempotencyKey,
            String requestBody,
            String requestType,
            Object response,
            int statusCode) {
        storeResponse(idempotencyKey, requestBody, requestType, response, statusCode, 24);
    }
    
    /**
     * Verify that the current request matches the stored request.
     * Prevents replay attacks with different request bodies but same idempotency key.
     * 
     * @param storedRequest The previously stored request
     * @param currentRequestBody The current request body
     * @return true if the requests match, false otherwise
     */
    public boolean verifyRequestMatch(IdempotentRequest storedRequest, String currentRequestBody) {
        String currentHash = computeHash(currentRequestBody);
        return storedRequest.getRequestHash().equals(currentHash);
    }
    
    /**
     * Clean up expired idempotency records.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredRecords() {
        int deleted = repository.deleteExpiredRequests(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
    }
    
    /**
     * Compute SHA-256 hash of request body.
     */
    private String computeHash(String input) {
        if (input == null) {
            input = "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return String.valueOf(input.hashCode());
        }
    }
}
