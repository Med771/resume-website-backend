ALTER TABLE specialities
    ADD CONSTRAINT uc_specialities_name UNIQUE (name);
