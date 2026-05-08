-- Persiste le type de question et son caractere obligatoire.
ALTER TABLE public.chatbot_question
    ADD COLUMN IF NOT EXISTS answer_type TEXT NOT NULL DEFAULT 'open',
    ADD COLUMN IF NOT EXISTS is_required BOOLEAN NOT NULL DEFAULT TRUE;
