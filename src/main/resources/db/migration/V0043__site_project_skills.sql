CREATE TABLE IF NOT EXISTS site_project_skills
(
    site_project_id UUID   NOT NULL,
    skill_id        BIGINT NOT NULL,
    CONSTRAINT pk_site_project_skills PRIMARY KEY (site_project_id, skill_id),
    CONSTRAINT fk_site_project_skills_project FOREIGN KEY (site_project_id)
        REFERENCES site_projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_site_project_skills_skill FOREIGN KEY (skill_id)
        REFERENCES skills (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_site_project_skills_skill ON site_project_skills (skill_id);
