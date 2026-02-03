#!/bin/bash

# Verification script for E-Commerce Microservices

set -e

echo "=========================================="
echo "E-Commerce Microservices Verification"
echo "=========================================="

API_GATEWAY="http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# Check if services are running
echo ""
echo "Step 1: Checking if services are running..."
if curl -s -o /dev/null -w "%{http_code}" "$API_GATEWAY/actuator/health" 2>/dev/null || curl -s "$API_GATEWAY/api/users/register" -X POST -H "Content-Type: application/json" -d '{}' > /dev/null 2>&1; then
    print_status "API Gateway is accessible"
else
    print_error "API Gateway is not accessible. Please start services with: docker-compose up -d"
    exit 1
fi

# Test 1: User Registration
echo ""
echo "Step 2: Testing User Registration..."
REGISTER_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_'$(date +%s)'@example.com",
    "password": "password123",
    "fullName": "Test User"
  }' 2>/dev/null)

if echo "$REGISTER_RESPONSE" | grep -q "token"; then
    print_status "User registration successful"
    USER_EMAIL=$(echo "$REGISTER_RESPONSE" | grep -o '"email":"[^"]*"' | cut -d'"' -f4)
    echo "  Created user: $USER_EMAIL"
else
    print_error "User registration failed"
    echo "  Response: $REGISTER_RESPONSE"
fi

# Test 2: User Login
echo ""
echo "Step 3: Testing User Login..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'$USER_EMAIL'",
    "password": "password123"
  }' 2>/dev/null)

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    print_status "User login successful"
    JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "  JWT Token received: ${JWT_TOKEN:0:20}..."
else
    print_error "User login failed"
    echo "  Response: $LOGIN_RESPONSE"
    exit 1
fi

# Test 3: Get User Profile
echo ""
echo "Step 4: Testing Get User Profile..."
PROFILE_RESPONSE=$(curl -s -X GET "$API_GATEWAY/api/users/profile" \
  -H "Authorization: Bearer $JWT_TOKEN" 2>/dev/null)

if echo "$PROFILE_RESPONSE" | grep -q "email"; then
    print_status "User profile retrieved successfully"
else
    print_error "Failed to retrieve user profile"
    echo "  Response: $PROFILE_RESPONSE"
fi

# Test 4: Create Order
echo ""
echo "Step 5: Testing Order Creation..."
ORDER_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "items": [
      {"productId": "prod-001", "quantity": 2, "price": 29.99},
      {"productId": "prod-002", "quantity": 1, "price": 49.99}
    ]
  }' 2>/dev/null)

if echo "$ORDER_RESPONSE" | grep -q "orderId"; then
    print_status "Order created successfully"
    ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
    echo "  Order ID: $ORDER_ID"
else
    print_error "Order creation failed"
    echo "  Response: $ORDER_RESPONSE"
    exit 1
fi

# Test 5: Get Order by ID
echo ""
echo "Step 6: Testing Get Order by ID..."
sleep 2  # Wait for async processing
ORDER_DETAIL_RESPONSE=$(curl -s -X GET "$API_GATEWAY/api/orders/$ORDER_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" 2>/dev/null)

if echo "$ORDER_DETAIL_RESPONSE" | grep -q "orderId"; then
    print_status "Order details retrieved successfully"
    ORDER_STATUS=$(echo "$ORDER_DETAIL_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo "  Order Status: $ORDER_STATUS"
else
    print_error "Failed to retrieve order details"
    echo "  Response: $ORDER_DETAIL_RESPONSE"
fi

# Test 6: Get Payment for Order
echo ""
echo "Step 7: Testing Get Payment..."
sleep 2  # Wait for payment processing
PAYMENT_RESPONSE=$(curl -s -X GET "$API_GATEWAY/api/payments/order/$ORDER_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" 2>/dev/null)

if echo "$PAYMENT_RESPONSE" | grep -q "paymentId"; then
    print_status "Payment details retrieved successfully"
    PAYMENT_STATUS=$(echo "$PAYMENT_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo "  Payment Status: $PAYMENT_STATUS"
else
    print_error "Failed to retrieve payment details"
    echo "  Response: $PAYMENT_RESPONSE"
fi

# Test 7: List User Orders
echo ""
echo "Step 8: Testing List User Orders..."
ORDERS_LIST_RESPONSE=$(curl -s -X GET "$API_GATEWAY/api/orders" \
  -H "Authorization: Bearer $JWT_TOKEN" 2>/dev/null)

if echo "$ORDERS_LIST_RESPONSE" | grep -q "orders"; then
    print_status "Orders list retrieved successfully"
else
    print_error "Failed to retrieve orders list"
    echo "  Response: $ORDERS_LIST_RESPONSE"
fi

# Summary
echo ""
echo "=========================================="
echo "Verification Complete!"
echo "=========================================="
print_info "Check notification-service logs for email notifications:"
echo "  docker-compose logs notification-service"
echo ""
print_info "Check order status (will be updated asynchronously):"
echo "  curl -H \"Authorization: Bearer $JWT_TOKEN\" $API_GATEWAY/api/orders/$ORDER_ID"
echo ""
print_info "Full flow test completed successfully!"
echo ""
