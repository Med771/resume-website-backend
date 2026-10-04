ALTER TABLE students ALTER COLUMN birth_date DROP NOT NULL;

UPDATE students SET catalog_visible = false WHERE course = '0';

UPDATE students SET course = NULL WHERE course = '0';
