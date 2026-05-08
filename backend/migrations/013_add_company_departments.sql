-- Services/départements d'une entreprise
CREATE TABLE company_departments (
  department_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id    UUID NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
  name          VARCHAR(100) NOT NULL,
  description   TEXT,
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- Lier les recruteurs à un département
ALTER TABLE recruiters ADD COLUMN department_id UUID REFERENCES company_departments(department_id) ON DELETE SET NULL;
