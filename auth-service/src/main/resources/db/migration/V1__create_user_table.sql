CREATE TABLE Users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    adresse VARCHAR(255),
    date_de_naissance DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);