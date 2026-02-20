-- V2__Add_Performance_Indexes.sql
-- Performance optimization indexes for User Service

-- ============================================================================
-- USERS TABLE INDEXES
-- ============================================================================

-- Email lookup: Authentication and duplicate checking
-- Supports: Login, registration email check
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email 
    ON users(email);

-- Created at: User registration reports
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_created_at 
    ON users(created_at DESC);

-- Role + Created: User analytics by role
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_role_created 
    ON users(role, created_at DESC);

-- ============================================================================
-- IDEMPOTENT_REQUESTS TABLE INDEXES (from Phase 1)
-- ============================================================================

-- Already has unique index on idempotency_key from V1
-- Add index for cleanup queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_idempotent_expires 
    ON idempotent_requests(expires_at) 
    WHERE expires_at < NOW();

-- ============================================================================
-- ANALYZE
-- ============================================================================

ANALYZE users;
ANALYZE idempotent_requests;
