-- Stocke les validations/corrections recruteur sur les constats d'analyse.
CREATE TABLE IF NOT EXISTS public.analysis_fact_feedback (
    feedback_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES public.applications(application_id) ON DELETE CASCADE,
    dimension TEXT NOT NULL,
    finding TEXT NOT NULL,
    evidence TEXT,
    decision TEXT NOT NULL,
    corrected_finding TEXT,
    reviewer_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_fact_feedback_application
    ON public.analysis_fact_feedback (application_id, created_at DESC);
