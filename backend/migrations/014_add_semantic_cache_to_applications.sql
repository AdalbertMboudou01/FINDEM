-- Migration 014: Add semantic analysis cache to applications
-- Avoids calling OpenAI on every list load; cached result served instantly

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS semantic_cache    JSONB,
    ADD COLUMN IF NOT EXISTS semantic_cache_at TIMESTAMP;
