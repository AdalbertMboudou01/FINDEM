-- =============================================================================
-- Migration 009: Commentaires internes avec mentions
-- =============================================================================

CREATE TABLE IF NOT EXISTS comments (
    id             uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id uuid        NOT NULL REFERENCES applications(application_id) ON DELETE CASCADE,
    company_id     uuid        NOT NULL REFERENCES companies(id),
    author_id      uuid        NOT NULL,
    author_type    text        NOT NULL DEFAULT 'RECRUITER' CHECK (author_type IN ('USER', 'RECRUITER')),
    body           text        NOT NULL,
    mentions       uuid[]      NOT NULL DEFAULT '{}',
    visibility     text        NOT NULL DEFAULT 'INTERNAL' CHECK (visibility IN ('INTERNAL', 'SHARED')),
    parent_id      uuid        REFERENCES comments(id) ON DELETE SET NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz
);

CREATE INDEX IF NOT EXISTS idx_comments_application_id ON comments(application_id);
CREATE INDEX IF NOT EXISTS idx_comments_company_id     ON comments(company_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id      ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at     ON comments(created_at DESC);
