CREATE TABLE users
(
    id            UUID NOT NULL,
    role          VARCHAR(32),
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    username      VARCHAR(64),
    email         VARCHAR(255),
    password_hash VARCHAR(128),
    CONSTRAINT pk_users PRIMARY KEY (id)
);
