ALTER TABLE students ADD COLUMN catalog_visible BOOLEAN NOT NULL DEFAULT true;

UPDATE students SET catalog_visible = false WHERE course = '0';
