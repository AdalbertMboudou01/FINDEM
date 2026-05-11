-- =============================================================================
-- Schéma complet — Findem (assistant de préqualification alternance)
-- Migration consolidée — remplace toutes les migrations précédentes
-- =============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─────────────────────────────────────────────────────────────────────────────
-- TYPES ENUM
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TYPE public.sentiment_type AS ENUM ('FAVORABLE', 'RESERVE', 'DEFAVORABLE');
CREATE TYPE public.notification_type AS ENUM (
  'MENTION', 'TASK_ASSIGNED', 'DECISION_NEEDED',
  'STATUS_CHANGED', 'COMMENT_ADDED', 'OPINION_SUBMITTED', 'FINAL_DECISION_RECORDED'
);
CREATE TYPE public.task_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'URGENT');
CREATE TYPE public.task_status   AS ENUM ('TODO', 'IN_PROGRESS', 'DONE');

-- ─────────────────────────────────────────────────────────────────────────────
-- UTILISATEURS & AUTHENTIFICATION
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.users (
  id       uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  email    varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  role     varchar(255) NOT NULL
);

CREATE TABLE public.password_reset_tokens (
  token_id   uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  token      varchar(255) NOT NULL UNIQUE,
  created_at timestamp    NOT NULL,
  expires_at timestamp    NOT NULL,
  used       boolean      NOT NULL,
  user_id    uuid         NOT NULL REFERENCES public.users(id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ORGANISATION
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.companies (
  company_id uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  name       varchar(255),
  sector     varchar(255),
  size       varchar(255),
  website    varchar(255),
  plan       varchar(255),
  config     jsonb,
  created_at timestamp
);

CREATE TABLE public.company_departments (
  department_id uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  name          varchar(255) NOT NULL,
  description   varchar(255),
  created_at    timestamp    NOT NULL,
  company_id    uuid         NOT NULL REFERENCES public.companies(company_id)
);

CREATE TABLE public.company_invitations (
  invitation_id    uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  email            varchar(255) NOT NULL,
  role             varchar(255) NOT NULL,
  status           varchar(255) NOT NULL,
  invitation_token varchar(255) NOT NULL UNIQUE,
  created_at       timestamp    NOT NULL,
  expires_at       timestamp    NOT NULL,
  accepted_at      timestamp,
  company_id       uuid         NOT NULL REFERENCES public.companies(company_id),
  department_id    uuid         REFERENCES public.company_departments(department_id),
  CONSTRAINT company_invitations_status_check CHECK (
    status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED')
  )
);

CREATE TABLE public.recruiters (
  recruiter_id uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  email        varchar(255),
  name         varchar(255),
  role         varchar(255),
  status       varchar(255),
  bio          text,
  phone        varchar(255),
  photo_url    varchar(255),
  auth_user_id uuid,
  company_id   uuid         REFERENCES public.companies(company_id),
  department_id uuid        REFERENCES public.company_departments(department_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- OFFRES
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.jobs (
  job_id              uuid    PRIMARY KEY DEFAULT uuid_generate_v4(),
  title               text,
  description         text,
  contexte_poste      text,
  missions_detaillees text,
  service_entreprise  text,
  location            text,
  duration_contract   text,
  slug                text,
  statut              text,
  auto_close          boolean NOT NULL DEFAULT true,
  max_candidatures    integer,
  blocking_criteria   jsonb,
  created_at          timestamp,
  company_id          uuid    REFERENCES public.companies(company_id),
  owner_recruiter_id  uuid    REFERENCES public.recruiters(recruiter_id)
);

CREATE TABLE public.job_technologies (
  job_job_id   uuid         NOT NULL REFERENCES public.jobs(job_id),
  technologies varchar(255)
);

CREATE TABLE public.job_assignees (
  job_id       uuid      NOT NULL REFERENCES public.jobs(job_id) ON DELETE CASCADE,
  recruiter_id uuid      NOT NULL REFERENCES public.recruiters(recruiter_id) ON DELETE CASCADE,
  assigned_at  timestamp NOT NULL DEFAULT now(),
  PRIMARY KEY (job_id, recruiter_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- CANDIDATS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.candidates (
  candidate_id     uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  first_name       varchar(255),
  last_name        varchar(255),
  email            varchar(255),
  phone            varchar(255),
  location         varchar(255),
  school           varchar(255),
  github_url       varchar(255),
  portfolio_url    varchar(255),
  cv_path          varchar(255),
  cv_file_name     varchar(255),
  cv_content_type  varchar(255),
  consent          boolean,
  in_pool          boolean,
  github_cache     jsonb,
  github_cache_at  timestamp,
  created_at       timestamp
);

CREATE TABLE public.candidate_files (
  file_id            uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  file_name          varchar(255),
  original_file_name varchar(255),
  file_path          varchar(255),
  file_type          varchar(255),
  mime_type          varchar(255),
  file_size          bigint,
  uploaded_at        timestamp,
  candidate_id       uuid         REFERENCES public.candidates(candidate_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- CANDIDATURES & STATUTS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.application_statuses (
  status_id uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  code      varchar(255) UNIQUE,
  label     varchar(255)
);

CREATE TABLE public.applications (
  application_id    uuid      PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at        timestamp,
  semantic_cache    jsonb,
  semantic_cache_at timestamptz,
  candidate_id      uuid      REFERENCES public.candidates(candidate_id),
  job_id            uuid      REFERENCES public.jobs(job_id),
  status_id         uuid      REFERENCES public.application_statuses(status_id)
);

CREATE TABLE public.application_summaries (
  summary_id          uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  recommended_action  varchar(255),
  motivation_level    varchar(255),
  technical_profile   varchar(255),
  experience_level    varchar(255),
  availability_status varchar(255),
  location_match      varchar(255),
  blocking_criteria   varchar(255),
  key_skills          varchar(255),
  positive_points     varchar(255),
  concerns            varchar(255),
  project_highlights  varchar(255),
  summary_text        varchar(255),
  generated_by        varchar(255),
  generated_at        timestamp,
  application_id      uuid         UNIQUE REFERENCES public.applications(application_id)
);

CREATE TABLE public.application_activities (
  id             uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_type     varchar(255) NOT NULL,
  actor_type     varchar(255) NOT NULL,
  actor_id       uuid,
  company_id     uuid         NOT NULL,
  visibility     varchar(255) NOT NULL,
  payload        jsonb,
  created_at     timestamp    NOT NULL,
  application_id uuid         NOT NULL REFERENCES public.applications(application_id),
  CONSTRAINT application_activities_actor_type_check CHECK (
    actor_type IN ('USER', 'RECRUITER', 'SYSTEM')
  ),
  CONSTRAINT application_activities_event_type_check CHECK (
    event_type IN (
      'COMMENT_ADDED', 'STATUS_CHANGED', 'TASK_CREATED', 'TASK_DONE',
      'INTERVIEW_SCHEDULED', 'DOCUMENT_ADDED', 'DECISION_RECORDED',
      'MENTION_TRIGGERED', 'CHATBOT_COMPLETED', 'AI_ANALYSIS_DONE',
      'ANSWER_SUBMITTED', 'BATCH_ANSWERS_SUBMITTED',
      'INTERVIEW_COMPLETED', 'INTERVIEW_CANCELLED'
    )
  )
);

-- ─────────────────────────────────────────────────────────────────────────────
-- CHATBOT
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.chatbot_question (
  id            uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  question_text varchar(255) NOT NULL,
  question_key  varchar(255),
  answer_type   varchar(255) NOT NULL,
  order_index   integer      NOT NULL,
  is_required   boolean      NOT NULL,
  job_id        uuid         REFERENCES public.jobs(job_id)
);

CREATE TABLE public.chat_question_configs (
  config_id           uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  question_key        varchar(255) NOT NULL,
  question_text       text         NOT NULL,
  question_type       varchar(255) NOT NULL,
  order_index         integer,
  required            boolean      NOT NULL,
  adaptive_enabled    boolean      NOT NULL,
  follow_up_questions text,
  created_at          timestamp    NOT NULL,
  updated_at          timestamp,
  job_id              uuid         NOT NULL REFERENCES public.jobs(job_id)
);

CREATE TABLE public.chat_sessions (
  chat_session_id  uuid      PRIMARY KEY DEFAULT uuid_generate_v4(),
  scenario         varchar(255),
  language         varchar(255),
  progress         integer,
  completion_score integer,
  abandoned        boolean,
  started_at       timestamp,
  ended_at         timestamp,
  application_id   uuid      REFERENCES public.applications(application_id)
);

CREATE TABLE public.chat_answers (
  answer_id        uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  question_key     varchar(255) NOT NULL,
  question_text    text         NOT NULL,
  answer_text      text         NOT NULL,
  normalized_value varchar(255),
  required         boolean      NOT NULL,
  answered_at      timestamp,
  created_at       timestamp    NOT NULL,
  updated_at       timestamp,
  application_id   uuid         NOT NULL REFERENCES public.applications(application_id),
  chat_session_id  uuid         REFERENCES public.chat_sessions(chat_session_id)
);

CREATE TABLE public.chat_messages (
  message_id    uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_type  varchar(32),
  question_key  varchar(120),
  question_text text,
  answer        text,
  created_at    timestamp,
  application_id uuid        REFERENCES public.applications(application_id),
  candidate_id   uuid        REFERENCES public.candidates(candidate_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- COMMENTAIRES & AVIS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.comments (
  id             uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  body           text         NOT NULL,
  author_id      uuid         NOT NULL,
  author_type    varchar(255) NOT NULL,
  visibility     varchar(255) NOT NULL,
  company_id     uuid         NOT NULL,
  mentions       uuid[],
  created_at     timestamp    NOT NULL,
  updated_at     timestamp,
  application_id uuid         NOT NULL REFERENCES public.applications(application_id),
  parent_id      uuid         REFERENCES public.comments(id),
  CONSTRAINT comments_author_type_check CHECK (
    author_type IN ('USER', 'RECRUITER', 'AI_SYSTEM')
  ),
  CONSTRAINT comments_visibility_check CHECK (
    visibility IN ('INTERNAL', 'SHARED')
  )
);

CREATE TABLE public.decision_inputs (
  id             uuid                   PRIMARY KEY DEFAULT uuid_generate_v4(),
  sentiment      public.sentiment_type  NOT NULL,
  comment        text,
  confidence     integer,
  author_id      uuid                   NOT NULL,
  company_id     uuid                   NOT NULL,
  created_at     timestamp              NOT NULL,
  application_id uuid                   NOT NULL
);

CREATE TABLE public.decisions (
  id             uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  final_status   varchar(255) NOT NULL,
  rationale      text,
  ai_review      text,
  decided_at     timestamp    NOT NULL,
  decided_by     uuid         NOT NULL,
  application_id uuid         NOT NULL UNIQUE,
  company_id     uuid         NOT NULL
);

CREATE TABLE public.analysis_fact_feedback (
  feedback_id       uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  dimension         varchar(255) NOT NULL,
  finding           text         NOT NULL,
  decision          varchar(255) NOT NULL,
  evidence          text,
  corrected_finding text,
  reviewer_comment  text,
  created_at        timestamp    NOT NULL,
  application_id    uuid         NOT NULL REFERENCES public.applications(application_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ENTRETIENS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.interviews (
  interview_id     uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  title            varchar(255) NOT NULL,
  status           varchar(255) NOT NULL,
  scheduled_at     timestamp    NOT NULL,
  duration_minutes integer,
  location         varchar(255),
  meeting_url      varchar(255),
  description      text,
  notes            text,
  created_at       timestamp    NOT NULL,
  updated_at       timestamp,
  candidate_id     uuid         NOT NULL REFERENCES public.candidates(candidate_id),
  job_id           uuid         NOT NULL REFERENCES public.jobs(job_id),
  recruiter_id     uuid         NOT NULL REFERENCES public.recruiters(recruiter_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TÂCHES
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.tasks (
  id             uuid                  PRIMARY KEY DEFAULT uuid_generate_v4(),
  title          varchar(255)          NOT NULL,
  description    text,
  task_type      varchar(255),
  status         public.task_status    NOT NULL,
  priority       public.task_priority  NOT NULL,
  due_date       date,
  ai_result      text,
  assignee_id    uuid,
  created_by     uuid                  NOT NULL,
  company_id     uuid                  NOT NULL,
  created_at     timestamp             NOT NULL,
  updated_at     timestamp             NOT NULL,
  application_id uuid                  NOT NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- NOTIFICATIONS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.internal_notifications (
  id             uuid                       PRIMARY KEY DEFAULT uuid_generate_v4(),
  type           public.notification_type   NOT NULL,
  title          varchar(255)               NOT NULL,
  message        text,
  reference_type varchar(255),
  reference_id   uuid,
  user_id        uuid                       NOT NULL,
  company_id     uuid                       NOT NULL,
  read_at        timestamp,
  created_at     timestamp                  NOT NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- MESSAGES ÉQUIPE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.team_channels (
  channel_id   uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  name         varchar(255),
  channel_type varchar(255),
  created_at   timestamp,
  company_id   uuid,
  job_id       uuid,
  recruiter_id uuid
);

CREATE TABLE public.team_messages (
  message_id  uuid         PRIMARY KEY DEFAULT uuid_generate_v4(),
  content     text,
  author_id   uuid,
  author_name varchar(255),
  author_type varchar(255),
  mentions    jsonb,
  created_at  timestamp,
  channel_id  uuid         REFERENCES public.team_channels(channel_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- VOTES COLLECTIFS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE public.collective_votes (
  id             uuid      PRIMARY KEY DEFAULT gen_random_uuid(),
  round          integer   NOT NULL DEFAULT 1,
  status         varchar   NOT NULL DEFAULT 'OPEN',
  result         varchar,
  reminder_sent  boolean   NOT NULL DEFAULT false,
  opened_at      timestamp NOT NULL DEFAULT now(),
  closes_at      timestamp NOT NULL,
  closed_at      timestamp,
  created_at     timestamp NOT NULL DEFAULT now(),
  application_id uuid      NOT NULL REFERENCES public.applications(application_id) ON DELETE CASCADE,
  company_id     uuid      NOT NULL REFERENCES public.companies(company_id) ON DELETE CASCADE,
  CONSTRAINT collective_votes_status_check CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
  CONSTRAINT collective_votes_result_check CHECK (result IN ('APPROVED', 'REJECTED', 'TIE')),
  CONSTRAINT collective_votes_round_check  CHECK (round IN (1, 2))
);

CREATE TABLE public.collective_vote_ballots (
  id           uuid      PRIMARY KEY DEFAULT gen_random_uuid(),
  choice       varchar   NOT NULL,
  voted_at     timestamp NOT NULL DEFAULT now(),
  vote_id      uuid      NOT NULL REFERENCES public.collective_votes(id) ON DELETE CASCADE,
  voter_id     uuid      NOT NULL REFERENCES public.recruiters(recruiter_id),
  UNIQUE (vote_id, voter_id),
  CONSTRAINT collective_vote_ballots_choice_check CHECK (choice IN ('APPROVE', 'REJECT', 'ABSTAIN'))
);

CREATE TABLE public.final_votes (
  id             uuid      PRIMARY KEY DEFAULT gen_random_uuid(),
  choice         varchar   NOT NULL,
  voter_role     varchar   NOT NULL,
  rationale      text,
  voted_at       timestamp NOT NULL DEFAULT now(),
  application_id uuid      NOT NULL REFERENCES public.applications(application_id) ON DELETE CASCADE,
  company_id     uuid      NOT NULL REFERENCES public.companies(company_id) ON DELETE CASCADE,
  voter_id       uuid      NOT NULL REFERENCES public.recruiters(recruiter_id),
  UNIQUE (application_id, voter_role),
  CONSTRAINT final_votes_choice_check     CHECK (choice IN ('APPROVED', 'REJECTED')),
  CONSTRAINT final_votes_voter_role_check CHECK (voter_role IN ('MANAGER', 'ADMIN'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- INDEX
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX idx_applications_job_id       ON public.applications(job_id);
CREATE INDEX idx_applications_candidate_id ON public.applications(candidate_id);
CREATE INDEX idx_applications_status_id    ON public.applications(status_id);
CREATE INDEX idx_applications_created_at   ON public.applications(created_at);
CREATE INDEX idx_job_assignees_job_id      ON public.job_assignees(job_id);
CREATE INDEX idx_job_assignees_recruiter_id ON public.job_assignees(recruiter_id);
CREATE INDEX idx_collective_votes_application_id ON public.collective_votes(application_id);
CREATE INDEX idx_collective_votes_status   ON public.collective_votes(status);
CREATE INDEX idx_collective_votes_closes_at ON public.collective_votes(closes_at);
CREATE INDEX idx_ballots_vote_id           ON public.collective_vote_ballots(vote_id);
CREATE INDEX idx_final_votes_application_id ON public.final_votes(application_id);
