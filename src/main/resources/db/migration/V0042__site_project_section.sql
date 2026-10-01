ALTER TABLE site_projects
    ADD COLUMN IF NOT EXISTS section VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_site_projects_section ON site_projects (section);
