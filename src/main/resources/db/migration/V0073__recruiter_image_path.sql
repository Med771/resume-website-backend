ALTER TABLE recruiters
    ADD COLUMN image_path VARCHAR(255);

ALTER TABLE recruiters
    ADD CONSTRAINT uc_recruiters_imagepath UNIQUE (image_path);
