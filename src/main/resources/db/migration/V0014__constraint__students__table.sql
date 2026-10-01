ALTER TABLE students
    ADD CONSTRAINT uc_students_email UNIQUE (email);

ALTER TABLE students
    ADD CONSTRAINT uc_students_imagepath UNIQUE (image_path);

ALTER TABLE students
    ADD CONSTRAINT uc_students_telegram_user UNIQUE (telegram_user_id);

ALTER TABLE students
    ADD CONSTRAINT uc_students_username UNIQUE (username);

ALTER TABLE students
    ADD CONSTRAINT FK_STUDENTS_ON_SPECIALITY FOREIGN KEY (speciality_id) REFERENCES specialities (id);
