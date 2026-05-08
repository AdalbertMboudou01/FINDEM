ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_author_type_check;
ALTER TABLE comments ADD CONSTRAINT comments_author_type_check
  CHECK (author_type IN ('USER', 'RECRUITER', 'AI_SYSTEM'));
