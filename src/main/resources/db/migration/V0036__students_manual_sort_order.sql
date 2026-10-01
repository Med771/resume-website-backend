-- Ручной приоритет в списках каталога: меньшее число выше при ASC; NULL — без явного приоритета (в конце при ASC на PostgreSQL).
ALTER TABLE students
    ADD COLUMN IF NOT EXISTS manual_sort_order INTEGER;

COMMENT ON COLUMN students.manual_sort_order IS 'Ручной порядок в сортировке каталога; задаётся админом (PUT/PATCH). NULL = не задан.';
