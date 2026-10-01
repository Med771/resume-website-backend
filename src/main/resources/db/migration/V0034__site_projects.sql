CREATE TABLE site_projects
(
    id                     UUID         NOT NULL,
    title                  VARCHAR(255) NOT NULL,
    summary                TEXT,
    body                   TEXT,
    image_path             VARCHAR(512),
    sort_order             INTEGER      NOT NULL DEFAULT 0,
    visible_to_anonymous   BOOLEAN      NOT NULL DEFAULT false,
    published_from         TIMESTAMP WITHOUT TIME ZONE,
    published_to           TIMESTAMP WITHOUT TIME ZONE,
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_site_projects PRIMARY KEY (id)
);

CREATE INDEX idx_site_projects_sort ON site_projects (sort_order);
CREATE INDEX idx_site_projects_public ON site_projects (visible_to_anonymous, sort_order);
