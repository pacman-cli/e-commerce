package com.ecommerce.user.service;

import java.util.UUID;

import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.UserResponse;

public interface UserService {
  AuthResponse registerUser(RegisterRequest request);

  AuthResponse loginUser(LoginRequest request);

  UserResponse getUserById(UUID userId);
}
