-- Optional demo seed (run after Flyway on empty DB)
-- Requires admin user from bootstrap

-- Example speciality with icon path (upload icons via admin separately)
INSERT INTO specialities (name, icon_path, created_at, updated_at)
SELECT 'Backend', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM specialities WHERE name = 'Backend');
