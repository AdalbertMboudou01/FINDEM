-- Ajout des colonnes de quota et fermeture automatique sur les offres.
ALTER TABLE public.jobs
    ADD COLUMN IF NOT EXISTS max_candidatures INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS auto_close       BOOLEAN NOT NULL DEFAULT TRUE;
