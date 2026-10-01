-- Галерея изображений проектов. Переносит legacy image_path/image_url из site_projects.
-- pgcrypto — для gen_random_uuid() на PostgreSQL < 13 (на 13+ функция встроена, расширение безвредно).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS site_project_images
(
    id              UUID         NOT NULL,
    site_project_id UUID         NOT NULL,
    image_path      VARCHAR(512),
    image_url       VARCHAR(1024),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_site_project_images PRIMARY KEY (id),
    CONSTRAINT fk_site_project_images_project FOREIGN KEY (site_project_id)
        REFERENCES site_projects (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_site_project_images_project ON site_project_images (site_project_id, sort_order);

INSERT INTO site_project_images (id, site_project_id, image_path, image_url, sort_order)
SELECT gen_random_uuid(), sp.id, sp.image_path, sp.image_url, 0
FROM site_projects sp
WHERE (sp.image_path IS NOT NULL OR sp.image_url IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM site_project_images i WHERE i.site_project_id = sp.id
  );

ALTER TABLE site_projects DROP COLUMN IF EXISTS image_path;
ALTER TABLE site_projects DROP COLUMN IF EXISTS image_url;
