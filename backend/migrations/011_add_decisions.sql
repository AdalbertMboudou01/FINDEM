-- Bloc 4: Avis individuels et décision finale

CREATE TYPE sentiment_type AS ENUM ('FAVORABLE', 'RESERVE', 'DEFAVORABLE');

CREATE TABLE decision_inputs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(application_id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
    author_id       UUID NOT NULL,
    sentiment       sentiment_type NOT NULL,
    comment         TEXT,
    confidence      INT CHECK (confidence BETWEEN 1 AND 5),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE decisions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL UNIQUE REFERENCES applications(application_id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
    final_status    VARCHAR(100) NOT NULL,
    rationale       TEXT,
    decided_by      UUID NOT NULL,
    decided_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_decision_inputs_application_id ON decision_inputs(application_id);
CREATE INDEX idx_decisions_application_id ON decisions(application_id);
