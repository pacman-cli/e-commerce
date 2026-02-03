#!/bin/bash

# =============================================================================
# E-Commerce Microservices - Quick Setup Script
# =============================================================================
# This script automates the initialization of the entire microservices system.
# It handles:
# 1. Prerequisite checks (Docker, Compose)
# 2. Secure Environment Variable generation (.env)
# 3. Clean startup of containers
# 4. Robust health checking to ensure all services are actually ready
# =============================================================================

# 'set -e' tells the script to exit immediately if any command returns a non-zero status (failure).
# This prevents the script from snowballing errors (e.g., trying to start containers if build failed).
set -e

# Visual Setup: Helper functions for colored output to make logs readable
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     E-Commerce Microservices - Quick Setup                  ║"
echo "║     Automated Deployment & Health Verification              ║"
echo "╚══════════════════════════════════════════════════════════════╝"

# ANSI color codes for terminal output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color (reset)

# Helper function to print steps with a yellow timestamp/label
print_step() {
    echo -e "${YELLOW}[STEP $1]${NC} $2"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# =============================================================================
# STEP 1: PREREQUISITES
# =============================================================================
# We assume the user has Docker installed. If not, we fail fast.
print_step "1/6" "Checking prerequisites..."

# 'command -v' checks if a binary exists in the system PATH.
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

print_success "Prerequisites OK"

# =============================================================================
# STEP 2: ENVIRONMENT CONFIGURATION
# =============================================================================
# We need to ensure a valid .env file exists for Docker Compose to read variables from.
print_step "2/6" "Setting up environment..."

if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    # Copy the template file provided in the repo
    cp .env.example .env

    # SECURITY AUTOMATION:
    # Instead of asking the user to manually set passwords, we generate them.
    # 'openssl rand -base64 32' creates a cryptographically secure 32-byte string.
    JWT_SECRET=$(openssl rand -base64 32)
    DB_PASSWORD=$(openssl rand -base64 24)

    # Use 'sed' (Stream Editor) to find and replace the placeholder text in the new .env file.
    # The '-i.bak' creates a backup just in case, which we delete later.
    sed -i.bak "s/JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" .env
    sed -i.bak "s/DB_USER_PASSWORD=.*/DB_USER_PASSWORD=$DB_PASSWORD/" .env
    sed -i.bak "s/DB_ORDER_PASSWORD=.*/DB_ORDER_PASSWORD=$DB_PASSWORD/" .env
    sed -i.bak "s/DB_PAYMENT_PASSWORD=.*/DB_PAYMENT_PASSWORD=$DB_PASSWORD/" .env
    rm -f .env.bak

    print_success "Created .env file with secure secrets"
    echo "⚠️  IMPORTANT: Review and customize .env file for production use"
else
    print_success ".env file already exists"
fi

# Sanity check: Ensure the user didn't leave the dummy value in.
if ! grep -q "^JWT_SECRET=change-me" .env; then
    print_success "JWT_SECRET is configured"
else
    print_error "JWT_SECRET still has placeholder value. Please update .env file"
    exit 1
fi

# =============================================================================
# STEP 3: CLEANUP
# =============================================================================
# We remove old containers to ensure a fresh state.
# '-v': Removes volumes (wiping old DB data) - useful for dev, careful in prod!
# '--remove-orphans': Removes containers not defined in the current compose file.
print_step "3/6" "Cleaning up existing containers..."
docker-compose down -v --remove-orphans 2>/dev/null || true
print_success "Cleanup complete"

# =============================================================================
# STEP 4: BUILD & START
# =============================================================================
print_step "4/6" "Building and starting services..."
# '-d': Detached mode (run in background)
# '--build': Forces a rebuild of images (essential if you changed code)
docker-compose up -d --build

# =============================================================================
# STEP 5: HEALTH CHECK LOOP (CRITICAL)
# =============================================================================
# This is the most important part. Docker 'starting' != Java 'running'.
# We must poll the containers until their internal health checks pass.
print_step "5/6" "Waiting for services to start (this may take 60-90 seconds)..."
echo ""

# Loop for max 200 attempts * 2 seconds sleep = ~400 seconds timeout
for i in {1..200}; do
    sleep 2

    # INSPECT HEALTH:
    # 1. 'docker-compose ps -q': Gets IDs of all containers in this project.
    # 2. 'xargs docker inspect': Runs inspect on each ID.
    # 3. '-f ...Health.Status': Extracts just the health status string (healthy/unhealthy/starting).
    # 4. 'grep -c': Counts how many lines match "healthy".
    HEALTHY_COUNT=$(docker-compose ps -q | xargs docker inspect -f '{{.State.Health.Status}}' 2>/dev/null | grep -c "healthy" || true)

    # Count total containers needed
    TOTAL_CONTAINERS=$(docker-compose ps -q | wc -l)

    # VISUAL SPINNER: Creates a rotating bar character to show activity
    C=$((i % 4))
    case $C in
        0) SPINNER="-" ;;
        1) SPINNER="\\" ;;
        2) SPINNER="|" ;;
        3) SPINNER="/" ;;
    esac

    # '\r' moves cursor to start of line to overwrite the previous status update
    echo -ne "\r$SPINNER Waiting for services to be HEALTHY... $HEALTHY_COUNT/$TOTAL_CONTAINERS containers healthy (Attempt $i/200)"

    # LOGIC:
    # We expect Zookeeper, Kafka, 3 DBs, 3 Services, 1 Gateway = ~9 containers.
    # If most are healthy (>8), we check the Gateway specifically because it starts LAST.
    if [ "$HEALTHY_COUNT" -gt 8 ]; then
         # Check status of the 'api-gateway' service specifically
         GATEWAY_STATUS=$(docker-compose ps -q api-gateway | xargs docker inspect -f '{{.State.Health.Status}}' 2>/dev/null)

         # If Gateway is green, the whole system is up, because Gateway depends on everything else.
         if [ "$GATEWAY_STATUS" == "healthy" ]; then
            echo ""
            print_success "All critical services including Gateway are HEALTHY!"
            break
         fi
    fi

    # Timeout handler
    if [ $i -eq 200 ]; then
        echo ""
        print_error "Timeout waiting for services to become healthy."
        docker-compose ps
        exit 1
    fi
done

echo ""

# =============================================================================
# STEP 6: FINAL SMOKE TEST
# =============================================================================
# Even if Docker says healthy, we try one real HTTP request to be sure.
print_step "6/6" "Performing health check..."

sleep 5

# Try to hit an endpoint via the Gateway
for i in {1..10}; do
    # curl args:
    # -s: Silent (no progress bar)
    # -o /dev/null: Discard output body
    # -w "%{http_code}": Print only the HTTP status code
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        http://localhost:8080/api/users/register \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"email":"health@test.com","password":"test123","fullName":"Health Check"}' \
        2>/dev/null || echo "000")

    # We expect 400 (Bad Request) because the email might exist or data is partial,
    # OR 201 (Created) if it worked. Both mean the Java app handled the request.
    # 503 or 000 would mean failure.
    if [ "$HTTP_STATUS" = "400" ] || [ "$HTTP_STATUS" = "201" ]; then
        print_success "API Gateway is responding (HTTP $HTTP_STATUS)"
        break
    fi

    if [ $i -eq 10 ]; then
        print_error "API Gateway health check failed"
        echo "Check logs: docker-compose logs api-gateway"
        # Common troubleshooting tips provided in the shell output
        echo ""
        echo "Common issues:"
        echo "  1. JWT_SECRET not configured properly"
        echo "  2. Services still starting up"
        echo "  3. Port 8080 already in use"
    fi

    sleep 2
done

# =============================================================================
# FOOTER: SUMMARY & INFO
# =============================================================================
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                   🎉 Setup Complete!                         ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "Services are running at:"
echo "  • API Gateway:    http://localhost:8080"
echo "  • User Service:   http://localhost:8081"
echo "  • Order Service:  http://localhost:8082"
echo "  • Payment Service:http://localhost:8083"
echo "  • Notification:   http://localhost:8084"
echo "  • Kafka:          localhost:9092"
echo ""
echo "Quick commands:"
echo "  • View logs:      docker-compose logs -f [service]"
echo "  • Run tests:      ./verify.sh"
echo "  • Stop services:  docker-compose down -v"
echo ""
echo "Security Features Enabled:"
echo "  ✅ JWT token validation"
echo "  ✅ Rate limiting (60 req/min)"
echo "  ✅ CORS whitelist"
echo "  ✅ Security headers"
echo "  ✅ Request size limits"
echo "  ✅ Correlation ID tracking"
echo ""
echo "Register a test user:"
echo "  curl -X POST http://localhost:8080/api/users/register \\"
echo "    -H \"Content-Type: application/json\" \\"
echo "    -d '{\"email\":\"test@example.com\",\"password\":\"password123\",\"fullName\":\"Test User\"}'"
echo ""
echo "📖 Manuals:"
echo "  • Setup Guide:      LOCAL_SETUP.md"
echo "  • Testing Guide:    SWAGGER_TEST_GUIDE.md"
echo "  • Architecture:     PHASE_0_IMPLEMENTATION_SUMMARY.md"
echo ""
