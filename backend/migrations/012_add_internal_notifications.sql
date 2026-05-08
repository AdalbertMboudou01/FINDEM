-- Bloc 5: Notifications in-app orientées action

CREATE TYPE notification_type AS ENUM (
    'MENTION',
    'TASK_ASSIGNED',
    'DECISION_NEEDED',
    'STATUS_CHANGED',
    'COMMENT_ADDED'
);

CREATE TABLE internal_notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    company_id      UUID NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
    type            notification_type NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_internal_notifications_user_id ON internal_notifications(user_id);
CREATE INDEX idx_internal_notifications_company_id ON internal_notifications(company_id);
CREATE INDEX idx_internal_notifications_read_at ON internal_notifications(read_at);
