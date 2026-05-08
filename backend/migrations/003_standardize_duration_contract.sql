-- Standardize duration_contract values in jobs table.
-- Allowed values: 6 mois, 12 mois, 24 mois, 36 mois.

UPDATE public.jobs
SET duration_contract = CASE
  WHEN lower(coalesce(duration_contract, '')) IN ('6', '6m', '6 mois', '6 month', '6 months') THEN '6 mois'
  WHEN lower(coalesce(duration_contract, '')) IN ('12', '12m', '12 mois', '12 month', '12 months') THEN '12 mois'
  WHEN lower(coalesce(duration_contract, '')) IN ('24', '24m', '24 mois', '24 month', '24 months') THEN '24 mois'
  WHEN lower(coalesce(duration_contract, '')) IN ('36', '36m', '36 mois', '36 month', '36 months') THEN '36 mois'
  WHEN lower(coalesce(duration_contract, '')) = 'personnalise' THEN
    CASE lower(coalesce(blocking_criteria ->> 'contractDuration', ''))
      WHEN '6' THEN '6 mois'
      WHEN '6m' THEN '6 mois'
      WHEN '6 mois' THEN '6 mois'
      WHEN '12' THEN '12 mois'
      WHEN '12m' THEN '12 mois'
      WHEN '12 mois' THEN '12 mois'
      WHEN '24' THEN '24 mois'
      WHEN '24m' THEN '24 mois'
      WHEN '24 mois' THEN '24 mois'
      WHEN '36' THEN '36 mois'
      WHEN '36m' THEN '36 mois'
      WHEN '36 mois' THEN '36 mois'
      ELSE '12 mois'
    END
  ELSE '12 mois'
END;

ALTER TABLE public.jobs
  ADD CONSTRAINT chk_jobs_duration_contract
  CHECK (duration_contract IN ('6 mois', '12 mois', '24 mois', '36 mois'));

-- Cleanup transitional JSON key now that duration_contract is canonical.
UPDATE public.jobs
SET blocking_criteria = blocking_criteria - 'contractDuration'
WHERE blocking_criteria ? 'contractDuration';
