-- =============================================================================
-- Schema complet -- Assistant de prequalification alternance
-- 20 tables | RLS actif partout | Toutes les FK, indexes et policies inclus
-- =============================================================================

-- Extension UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  1. ORGANISATION                                                        ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- companies : structure organisationnelle (entreprise cliente)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS companies (
  id         uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  name       text        NOT NULL,
  sector     text        NOT NULL DEFAULT '',
  size       text        NOT NULL DEFAULT '',            -- ex: 'PME', '50-200', 'ETI'
  website    text        NOT NULL DEFAULT '',
  plan       text        NOT NULL DEFAULT 'free',        -- free | pro | enterprise
  config     jsonb       NOT NULL DEFAULT '{}'::jsonb,   -- parametres specifiques
  created_at timestamptz NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- recruiters : membres de l'equipe recrutement lies a une entreprise
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recruiters (
  id           uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  company_id   uuid        NOT NULL REFERENCES companies(id),
  email        text        NOT NULL,
  name         text        NOT NULL DEFAULT '',
  role         text        NOT NULL DEFAULT 'recruiter',  -- admin | manager | recruiter
  status       text        NOT NULL DEFAULT 'active',     -- active | inactive | invited
  auth_user_id uuid,
  created_at   timestamptz NOT NULL DEFAULT now(),
  UNIQUE(company_id, email)
);

CREATE INDEX IF NOT EXISTS idx_recruiters_company_id   ON recruiters(company_id);
CREATE INDEX IF NOT EXISTS idx_recruiters_auth_user_id ON recruiters(auth_user_id);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  2. OFFRES D'ALTERNANCE                                                 ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- offers : fiches de poste en alternance
-- Statuts : ouvert | pause | cloture
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS offers (
  id               uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  title            text        NOT NULL,
  context          text        NOT NULL DEFAULT '',       -- contexte equipe / stack
  missions         text        NOT NULL DEFAULT '',       -- description des missions
  service          text        NOT NULL DEFAULT '',       -- departement (ex: 'Engineering - Equipe Produit')
  location         text        NOT NULL DEFAULT '',       -- lieu (ex: 'Paris - Hybrid')
  rythme           text        NOT NULL DEFAULT '',       -- rythme alternance (ex: '3 sem / 1 sem')
  technologies     text[]      NOT NULL DEFAULT '{}'::text[],
  status           text        NOT NULL DEFAULT 'ouvert', -- ouvert | pause | cloture
  chatbot_url      text        NOT NULL DEFAULT '',
  candidates_count integer     NOT NULL DEFAULT 0,
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now(),
  user_id          uuid
);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  3. CANDIDATS                                                           ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- candidates : profils candidats avec synthese IA
-- tri_category : prioritaire | a_examiner | a_revoir | a_ecarter
-- status : en_attente | retenu_entretien | a_revoir_manuellement | non_retenu | vivier
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS candidates (
  id                   uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  first_name           text        NOT NULL,
  last_name            text        NOT NULL,
  email                text        NOT NULL,
  phone                text,                                           -- nullable
  cv_url               text,                                           -- nullable
  tri_category         text        NOT NULL DEFAULT 'a_examiner',
  status               text        NOT NULL DEFAULT 'en_attente',
  alternance_compatible boolean    NOT NULL DEFAULT false,
  disponibilite        text        NOT NULL DEFAULT '',
  rythme_alternance    text        NOT NULL DEFAULT '',
  motivation_summary   text        NOT NULL DEFAULT '',
  projet_cite          text        NOT NULL DEFAULT '',
  technologies         text[]      NOT NULL DEFAULT '{}'::text[],
  github_url           text,                                           -- nullable
  portfolio_url        text,                                           -- nullable
  points_attention     text[]      NOT NULL DEFAULT '{}'::text[],
  action_recommandee   text        NOT NULL DEFAULT '',
  chatbot_responses    jsonb,                                          -- [{question, answer}]
  chatbot_completed    boolean     NOT NULL DEFAULT false,
  offer_id             uuid        REFERENCES offers(id),              -- nullable
  user_id              uuid,                                          -- nullable
  created_at           timestamptz NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- candidate_notes : notes internes sur un candidat (onglet Notes)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS candidate_notes (
  id           uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  candidate_id uuid        NOT NULL REFERENCES candidates(id),
  content      text        NOT NULL,
  author_name  text        NOT NULL DEFAULT '',
  user_id      uuid,
  created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_candidate_notes_candidate_id ON candidate_notes(candidate_id);

-- -----------------------------------------------------------------------------
-- candidate_links : liens externes (GitHub, portfolio, LinkedIn...)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS candidate_links (
  id           uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  candidate_id uuid        NOT NULL REFERENCES candidates(id),
  link_type    text        NOT NULL DEFAULT '',   -- github | portfolio | linkedin | other
  url          text        NOT NULL DEFAULT '',
  validated    boolean     NOT NULL DEFAULT false,
  created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_candidate_links_candidate_id ON candidate_links(candidate_id);

-- -----------------------------------------------------------------------------
-- candidate_projects : projets techniques cites par les candidats
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS candidate_projects (
  id           uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  candidate_id uuid        NOT NULL REFERENCES candidates(id),
  name         text        NOT NULL DEFAULT '',
  source_type  text        NOT NULL DEFAULT '',           -- github | gitlab | manual
  url          text        NOT NULL DEFAULT '',
  technologies text[]      NOT NULL DEFAULT '{}'::text[],
  has_readme   boolean     NOT NULL DEFAULT false,
  summary      text        NOT NULL DEFAULT '',
  created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_candidate_projects_candidate_id ON candidate_projects(candidate_id);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  4. CHATBOT DE PREQUALIFICATION                                         ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- chatbot_configs : configuration du chatbot par offre (1 config par offre)
-- questions stockees en JSONB : [{id, text, type, required, options?, order}]
-- types de question : open | choice | url | boolean | file
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chatbot_configs (
  id              uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  offer_id        uuid        UNIQUE REFERENCES offers(id),  -- 1 config par offre
  questions       jsonb       NOT NULL DEFAULT '[]'::jsonb,
  welcome_message text        NOT NULL DEFAULT '',
  closing_message text        NOT NULL DEFAULT '',
  active          boolean     NOT NULL DEFAULT true,
  user_id         uuid,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  5. ENTRETIENS                                                          ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- interviews : entretiens planifies
-- status : planifie | confirme | termine | annule
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interviews (
  id               uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  candidate_id     uuid        REFERENCES candidates(id),
  offer_id         uuid        REFERENCES offers(id),
  scheduled_at     timestamptz,                              -- nullable si pas encore planifie
  duration_minutes integer     NOT NULL DEFAULT 30,
  location         text        NOT NULL DEFAULT '',          -- 'Google Meet', 'Bureau A3', etc.
  notes            text        NOT NULL DEFAULT '',
  status           text        NOT NULL DEFAULT 'planifie',
  user_id          uuid,
  created_at       timestamptz NOT NULL DEFAULT now()
);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  6. DISCUSSIONS (architecture canaux style Teams)                        ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- services : canaux permanents de discussion
-- ex: 'Engineering - Equipe Produit', 'Direction', 'Qualite - QA'
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS services (
  id          uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  name        text        NOT NULL,
  description text        NOT NULL DEFAULT '',
  user_id     uuid,
  created_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE(name, user_id)
);

-- -----------------------------------------------------------------------------
-- discussions : sujets / fils de discussion dans un service
-- Peut etre lie a une offre et/ou un candidat
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussions (
  id           uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  title        text        NOT NULL,
  content      text        NOT NULL DEFAULT '',
  service      text        NOT NULL DEFAULT '',            -- nom du service en texte (affichage frontend)
  service_id   uuid        REFERENCES services(id),        -- lien structurel vers le canal
  offer_id     uuid        REFERENCES offers(id),          -- nullable
  candidate_id uuid        REFERENCES candidates(id),      -- nullable
  user_id      uuid,
  created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_discussions_service_id ON discussions(service_id);

-- -----------------------------------------------------------------------------
-- discussion_messages : messages individuels dans un fil de discussion
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_messages (
  id            uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  discussion_id uuid        NOT NULL REFERENCES discussions(id),
  content       text        NOT NULL,
  author_name   text        NOT NULL DEFAULT '',           -- nom affiche (ex: 'Marie Dupont')
  user_id       uuid,
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_discussion_messages_discussion_id ON discussion_messages(discussion_id);

-- -----------------------------------------------------------------------------
-- discussion_files : pieces jointes sur les messages
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_files (
  id           uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id   uuid        NOT NULL REFERENCES discussion_messages(id),
  file_name    text        NOT NULL,
  file_size    integer     NOT NULL DEFAULT 0,             -- taille en octets
  file_type    text        NOT NULL DEFAULT '',            -- MIME type
  storage_path text        NOT NULL DEFAULT '',            -- chemin Supabase Storage
  user_id      uuid,
  created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_discussion_files_message_id ON discussion_files(message_id);

-- -----------------------------------------------------------------------------
-- message_reactions : reactions emoji sur les messages
-- Contrainte unique : 1 reaction par emoji par utilisateur par message
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message_reactions (
  id         uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id uuid        NOT NULL REFERENCES discussion_messages(id),
  emoji      text        NOT NULL,
  user_id    uuid        NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(message_id, emoji, user_id)
);

CREATE INDEX IF NOT EXISTS idx_message_reactions_message_id ON message_reactions(message_id);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  7. ANNONCES INTERNES                                                   ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- announcements : communications internes
-- priority : normal | important | urgent
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS announcements (
  id         uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  title      text        NOT NULL,
  content    text        NOT NULL DEFAULT '',
  priority   text        NOT NULL DEFAULT 'normal',       -- normal | important | urgent
  user_id    uuid,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  8. VIVIER (TALENT POOLS)                                               ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- talent_pools : groupes de vivier structures
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS talent_pools (
  id          uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  name        text        NOT NULL,
  description text        NOT NULL DEFAULT '',
  user_id     uuid,
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- talent_pool_entries : candidats places dans un vivier
-- Contrainte unique : un candidat ne peut etre qu'une fois par pool
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS talent_pool_entries (
  id             uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  talent_pool_id uuid        NOT NULL REFERENCES talent_pools(id),
  candidate_id   uuid        NOT NULL REFERENCES candidates(id),
  reason         text        NOT NULL DEFAULT '',
  note           text        NOT NULL DEFAULT '',
  active         boolean     NOT NULL DEFAULT true,
  added_at       timestamptz NOT NULL DEFAULT now(),
  UNIQUE(talent_pool_id, candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_talent_pool_entries_pool_id      ON talent_pool_entries(talent_pool_id);
CREATE INDEX IF NOT EXISTS idx_talent_pool_entries_candidate_id ON talent_pool_entries(candidate_id);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  9. NOTIFICATIONS                                                       ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- notifications : alertes et notifications utilisateur
-- type : info | success | warning | error
-- reference_type + reference_id : lien polymorphe vers l'entite concernee
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
  id                uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  recipient_user_id uuid        NOT NULL,
  type              text        NOT NULL DEFAULT 'info',
  title             text        NOT NULL DEFAULT '',
  message           text        NOT NULL DEFAULT '',
  read              boolean     NOT NULL DEFAULT false,
  reference_type    text        NOT NULL DEFAULT '',       -- 'candidate' | 'offer' | 'interview' | ...
  reference_id      uuid,                                  -- nullable
  created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read      ON notifications(recipient_user_id, read);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  10. JOURNAL D'ACTIVITE                                                 ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- -----------------------------------------------------------------------------
-- activity_log : flux d'activite pour le dashboard
-- entity_type : 'candidate' | 'offer' | 'interview' | 'discussion' | ...
-- metadata : donnees contextuelles en JSONB
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_log (
  id          uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  action      text        NOT NULL,                        -- ex: 'candidature_recue', 'entretien_planifie'
  entity_type text        NOT NULL DEFAULT '',
  entity_id   uuid,                                        -- nullable
  metadata    jsonb       NOT NULL DEFAULT '{}'::jsonb,
  user_id     uuid,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_activity_log_user_id    ON activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_entity     ON activity_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_created_at ON activity_log(created_at DESC);

-- -----------------------------------------------------------------------------
-- application_statuses : statuts de candidature (compatibilité ancien code)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS application_statuses (
  status_id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  code text UNIQUE NOT NULL,
  label text NOT NULL
);

-- -----------------------------------------------------------------------------
-- applications : candidatures (compatibilité ancien code)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS applications (
  application_id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  candidate_id   uuid REFERENCES candidates(id) ON DELETE CASCADE,
  job_id         uuid REFERENCES offers(id) ON DELETE CASCADE,
  status_id      uuid REFERENCES application_statuses(status_id),
  created_at     timestamp DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- chat_sessions : sessions de chat (compatibilité ancien code)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_sessions (
  chat_session_id uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  application_id  uuid        REFERENCES applications(application_id) ON DELETE CASCADE,
  started_at      timestamptz DEFAULT now(),
  ended_at        timestamptz,
  scenario        text,
  language        text,
  progress        integer,
  completion_score integer,
  abandoned       boolean
);

-- -----------------------------------------------------------------------------
-- chat_questions : questions du chatbot (compatibilité ancien code)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_questions (
  question_id      uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  text             text        NOT NULL,
  answer_type      text,
  "order"          integer,
  required         boolean,
  category         text,
  scenario_version text
);

-- -----------------------------------------------------------------------------
-- chat_answers : réponses du chatbot (compatibilité ancien code)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_answers (
  chat_answer_id  uuid        PRIMARY KEY DEFAULT uuid_generate_v4(),
  chat_session_id uuid        REFERENCES chat_sessions(chat_session_id) ON DELETE CASCADE,
  question_id     uuid        REFERENCES chat_questions(question_id),
  answer_text     text,
  normalized_value text,
  answered_at     timestamptz DEFAULT now(),
  required        boolean
);

-- Index pour les tables de compatibilité
CREATE INDEX IF NOT EXISTS idx_applications_job_id        ON applications(job_id);
CREATE INDEX IF NOT EXISTS idx_applications_status_id     ON applications(status_id);
CREATE INDEX IF NOT EXISTS idx_applications_candidate_id  ON applications(candidate_id);
CREATE INDEX IF NOT EXISTS idx_applications_created_at    ON applications(created_at);

-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  11. ROW LEVEL SECURITY (RLS) - POLITIQUES DE SECURITE                    ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- Activer RLS sur toutes les tables
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE recruiters ENABLE ROW LEVEL SECURITY;
ALTER TABLE offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE chatbot_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE interviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE services ENABLE ROW LEVEL SECURITY;
ALTER TABLE discussions ENABLE ROW LEVEL SECURITY;
ALTER TABLE discussion_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE discussion_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE talent_pools ENABLE ROW LEVEL SECURITY;
ALTER TABLE talent_pool_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_statuses ENABLE ROW LEVEL SECURITY;
ALTER TABLE applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_answers ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- Politiques RLS pour companies
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own company"
  ON companies FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM recruiters
      WHERE recruiters.company_id = companies.id
        AND recruiters.auth_user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Authenticated users can create companies"
  ON companies FOR INSERT
  WITH CHECK (true);

CREATE POLICY "Admins can update own company"
  ON companies FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM recruiters
      WHERE recruiters.company_id = companies.id
        AND recruiters.auth_user_id = current_setting('app.current_user_id')::uuid
        AND recruiters.role = 'admin'
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM recruiters
      WHERE recruiters.company_id = companies.id
        AND recruiters.auth_user_id = current_setting('app.current_user_id')::uuid
        AND recruiters.role = 'admin'
    )
  );

-- -----------------------------------------------------------------------------
-- Politiques RLS pour recruiters
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view recruiters in own company"
  ON recruiters FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM recruiters AS r
      WHERE r.auth_user_id = current_setting('app.current_user_id')::uuid
        AND r.company_id = recruiters.company_id
    )
  );

CREATE POLICY "Users can insert recruiters in own company"
  ON recruiters FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM recruiters AS r
      WHERE r.auth_user_id = current_setting('app.current_user_id')::uuid
        AND r.company_id = recruiters.company_id
        AND r.role IN ('admin', 'manager')
    )
  );

CREATE POLICY "Users can update recruiters in own company"
  ON recruiters FOR UPDATE
  USING (
    auth_user_id = current_setting('app.current_user_id')::uuid
    OR EXISTS (
      SELECT 1 FROM recruiters AS r
      WHERE r.auth_user_id = current_setting('app.current_user_id')::uuid
        AND r.company_id = recruiters.company_id
        AND r.role IN ('admin', 'manager')
    )
  )
  WITH CHECK (
    auth_user_id = current_setting('app.current_user_id')::uuid
    OR EXISTS (
      SELECT 1 FROM recruiters AS r
      WHERE r.auth_user_id = current_setting('app.current_user_id')::uuid
        AND r.company_id = recruiters.company_id
        AND r.role IN ('admin', 'manager')
    )
  );

CREATE POLICY "Admins can delete recruiters in own company"
  ON recruiters FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM recruiters AS r
      WHERE r.auth_user_id = current_setting('app.current_user_id')::uuid
        AND r.company_id = recruiters.company_id
        AND r.role = 'admin'
    )
  );

-- -----------------------------------------------------------------------------
-- Politiques RLS pour offers
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own offers"
  ON offers FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own offers"
  ON offers FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own offers"
  ON offers FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own offers"
  ON offers FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour candidates
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own candidates"
  ON candidates FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own candidates"
  ON candidates FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own candidates"
  ON candidates FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own candidates"
  ON candidates FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour candidate_notes
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view notes on own candidates"
  ON candidate_notes FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_notes.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert notes on own candidates"
  ON candidate_notes FOR INSERT
  WITH CHECK (
    user_id = current_setting('app.current_user_id')::uuid
    AND EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_notes.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can update own notes"
  ON candidate_notes FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own notes"
  ON candidate_notes FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour candidate_links
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view links on own candidates"
  ON candidate_links FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_links.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert links on own candidates"
  ON candidate_links FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_links.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can delete links on own candidates"
  ON candidate_links FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_links.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

-- -----------------------------------------------------------------------------
-- Politiques RLS pour candidate_projects
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view projects on own candidates"
  ON candidate_projects FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_projects.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert projects on own candidates"
  ON candidate_projects FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_projects.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can delete projects on own candidates"
  ON candidate_projects FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM candidates
      WHERE candidates.id = candidate_projects.candidate_id
        AND candidates.user_id = current_setting('app.current_user_id')::uuid
    )
  );

-- -----------------------------------------------------------------------------
-- Politiques RLS pour chatbot_configs
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own chatbot configs"
  ON chatbot_configs FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own chatbot configs"
  ON chatbot_configs FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own chatbot configs"
  ON chatbot_configs FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own chatbot configs"
  ON chatbot_configs FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour interviews
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own interviews"
  ON interviews FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own interviews"
  ON interviews FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own interviews"
  ON interviews FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own interviews"
  ON interviews FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour services
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own services"
  ON services FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own services"
  ON services FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own services"
  ON services FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own services"
  ON services FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour discussions
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own discussions"
  ON discussions FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own discussions"
  ON discussions FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own discussions"
  ON discussions FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own discussions"
  ON discussions FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour discussion_messages
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view messages in own discussions"
  ON discussion_messages FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM discussions
      WHERE discussions.id = discussion_messages.discussion_id
        AND discussions.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert messages in own discussions"
  ON discussion_messages FOR INSERT
  WITH CHECK (
    user_id = current_setting('app.current_user_id')::uuid
    AND EXISTS (
      SELECT 1 FROM discussions
      WHERE discussions.id = discussion_messages.discussion_id
        AND discussions.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can update own messages"
  ON discussion_messages FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own messages"
  ON discussion_messages FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour discussion_files
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view files in own discussions"
  ON discussion_files FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM discussion_messages
      JOIN discussions ON discussions.id = discussion_messages.discussion_id
      WHERE discussion_messages.id = discussion_files.message_id
        AND discussions.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert files on own messages"
  ON discussion_files FOR INSERT
  WITH CHECK (
    user_id = current_setting('app.current_user_id')::uuid
    AND EXISTS (
      SELECT 1 FROM discussion_messages
      WHERE discussion_messages.id = discussion_files.message_id
        AND discussion_messages.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can delete own files"
  ON discussion_files FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour message_reactions
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view reactions in own discussions"
  ON message_reactions FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM discussion_messages
      JOIN discussions ON discussions.id = discussion_messages.discussion_id
      WHERE discussion_messages.id = message_reactions.message_id
        AND discussions.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert own reactions"
  ON message_reactions FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own reactions"
  ON message_reactions FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour announcements
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own announcements"
  ON announcements FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own announcements"
  ON announcements FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own announcements"
  ON announcements FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own announcements"
  ON announcements FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour talent_pools
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own talent pools"
  ON talent_pools FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own talent pools"
  ON talent_pools FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can update own talent pools"
  ON talent_pools FOR UPDATE
  USING (user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own talent pools"
  ON talent_pools FOR DELETE
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour talent_pool_entries
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view entries in own pools"
  ON talent_pool_entries FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM talent_pools
      WHERE talent_pools.id = talent_pool_entries.talent_pool_id
        AND talent_pools.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can insert entries in own pools"
  ON talent_pool_entries FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM talent_pools
      WHERE talent_pools.id = talent_pool_entries.talent_pool_id
        AND talent_pools.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can update entries in own pools"
  ON talent_pool_entries FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM talent_pools
      WHERE talent_pools.id = talent_pool_entries.talent_pool_id
        AND talent_pools.user_id = current_setting('app.current_user_id')::uuid
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM talent_pools
      WHERE talent_pools.id = talent_pool_entries.talent_pool_id
        AND talent_pools.user_id = current_setting('app.current_user_id')::uuid
    )
  );

CREATE POLICY "Users can delete entries from own pools"
  ON talent_pool_entries FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM talent_pools
      WHERE talent_pools.id = talent_pool_entries.talent_pool_id
        AND talent_pools.user_id = current_setting('app.current_user_id')::uuid
    )
  );

-- -----------------------------------------------------------------------------
-- Politiques RLS pour notifications
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own notifications"
  ON notifications FOR SELECT
  USING (recipient_user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "System can insert notifications for any user"
  ON notifications FOR INSERT
  WITH CHECK (recipient_user_id IS NOT NULL);

CREATE POLICY "Users can update own notifications"
  ON notifications FOR UPDATE
  USING (recipient_user_id = current_setting('app.current_user_id')::uuid)
  WITH CHECK (recipient_user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can delete own notifications"
  ON notifications FOR DELETE
  USING (recipient_user_id = current_setting('app.current_user_id')::uuid);

-- -----------------------------------------------------------------------------
-- Politiques RLS pour activity_log
-- -----------------------------------------------------------------------------
CREATE POLICY "Users can view own activity"
  ON activity_log FOR SELECT
  USING (user_id = current_setting('app.current_user_id')::uuid);

CREATE POLICY "Users can insert own activity"
  ON activity_log FOR INSERT
  WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);
