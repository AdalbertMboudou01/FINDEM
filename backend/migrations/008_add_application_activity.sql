-- =============================================================================
-- Migration 008: Timeline d'activité par candidature (append-only)
-- =============================================================================

CREATE TYPE application_event_type AS ENUM (
    'COMMENT_ADDED',
    'STATUS_CHANGED',
    'TASK_CREATED',
    'TASK_DONE',
    'INTERVIEW_SCHEDULED',
    'DOCUMENT_ADDED',
    'DECISION_RECORDED',
    'MENTION_TRIGGERED',
    'CHATBOT_COMPLETED',
    'AI_ANALYSIS_DONE'
);

CREATE TYPE actor_type AS ENUM ('USER', 'RECRUITER', 'SYSTEM');

CREATE TABLE IF NOT EXISTS application_activities (
    id             uuid                   PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id uuid                   NOT NULL REFERENCES applications(application_id) ON DELETE CASCADE,
    company_id     uuid                   NOT NULL REFERENCES companies(id),
    actor_id       uuid,
    actor_type     actor_type             NOT NULL DEFAULT 'SYSTEM',
    event_type     application_event_type NOT NULL,
    payload        jsonb                  NOT NULL DEFAULT '{}'::jsonb,
    visibility     text                   NOT NULL DEFAULT 'ALL' CHECK (visibility IN ('ALL', 'INTERNAL')),
    created_at     timestamptz            NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_app_activities_application_id ON application_activities(application_id);
CREATE INDEX IF NOT EXISTS idx_app_activities_company_id     ON application_activities(company_id);
CREATE INDEX IF NOT EXISTS idx_app_activities_created_at     ON application_activities(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_app_activities_event_type     ON application_activities(event_type);
