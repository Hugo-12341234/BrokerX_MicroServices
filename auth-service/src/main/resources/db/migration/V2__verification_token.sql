CREATE TABLE verification_token (
    id SERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    user_id BIGINT UNIQUE REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL
);
