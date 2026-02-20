package com.ecommerce.user.service.impl;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.shared.security.JwtUtil;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.InvalidCredentialsException;
import com.ecommerce.user.exception.UserAlreadyExistsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.outbox.OutboxEventPublisher;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserCreatedEventPayload;
import com.ecommerce.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final BCryptPasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final OutboxEventPublisher outboxPublisher;

        @Transactional
        public AuthResponse registerUser(RegisterRequest request) {
                log.info("Registering new user with email: {}", request.getEmail());

                if (userRepository.existsByEmail(request.getEmail())) {
                        log.warn("User already exists with email: {}", request.getEmail());
                        throw UserAlreadyExistsException.withEmail(request.getEmail());
                }

                User user = User.builder()
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName())
                                .role(Role.USER)
                                .build();

                User savedUser = userRepository.save(user);
                log.info("User registered successfully with ID: {}", savedUser.getId());

                // Create event payload for outbox pattern
                // This ensures atomicity: user and event are saved in same transaction
                UserCreatedEventPayload eventPayload = UserCreatedEventPayload.builder()
                                .userId(savedUser.getId())
                                .email(savedUser.getEmail())
                                .fullName(savedUser.getFullName())
                                .timestamp(System.currentTimeMillis())
                                .build();

                outboxPublisher.publish(
                                "User",
                                savedUser.getId().toString(),
                                "UserCreated",
                                eventPayload);

                log.info("UserCreatedEvent saved to outbox for user: {}", savedUser.getId());

                String token = jwtUtil.generateToken(
                                savedUser.getId(),
                                savedUser.getEmail(),
                                savedUser.getRole().name());

                return AuthResponse.builder()
                                .token(token)
                                .type("Bearer")
                                .user(mapToUserResponse(savedUser))
                                .build();
        }

        public AuthResponse loginUser(LoginRequest request) {
                log.info("Login attempt for email: {}", request.getEmail());

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> {
                                        log.warn("User not found with email: {}", request.getEmail());
                                        return InvalidCredentialsException.generic();
                                });

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        log.warn("Invalid password for email: {}", request.getEmail());
                        throw InvalidCredentialsException.generic();
                }

                log.info("User logged in successfully: {}", user.getId());

                String token = jwtUtil.generateToken(
                                user.getId(),
                                user.getEmail(),
                                user.getRole().name());

                return AuthResponse.builder()
                                .token(token)
                                .type("Bearer")
                                .user(mapToUserResponse(user))
                                .build();
        }

        public UserResponse getUserById(UUID userId) {
                log.info("Fetching user by ID: {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> {
                                        log.warn("User not found with ID: {}", userId);
                                        return UserNotFoundException.withId(userId.toString());
                                });

                return mapToUserResponse(user);
        }

        private UserResponse mapToUserResponse(User user) {
                return UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .role(user.getRole().name())
                                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                                .build();
        }
}
