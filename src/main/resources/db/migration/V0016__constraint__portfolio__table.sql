ALTER TABLE portfolio
    ADD CONSTRAINT uc_portfolio_name UNIQUE (name);

ALTER TABLE portfolio
    ADD CONSTRAINT FK_PORTFOLIO_ON_STUDENT FOREIGN KEY (student_id) REFERENCES students (id);
