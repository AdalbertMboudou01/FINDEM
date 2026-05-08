-- Rename jobs column from alternance_rhythm to duration_contract
-- Keeps existing values and avoids destructive schema changes.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'jobs'
      AND column_name = 'alternance_rhythm'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'jobs'
      AND column_name = 'duration_contract'
  ) THEN
    ALTER TABLE public.jobs RENAME COLUMN alternance_rhythm TO duration_contract;
  END IF;
END $$;
