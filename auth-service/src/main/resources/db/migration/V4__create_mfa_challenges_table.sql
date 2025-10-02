CREATE TABLE mfa_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    ip_address VARCHAR(45),
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    CONSTRAINT fk_mfa_challenges_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_mfa_challenges_user_id ON mfa_challenges(user_id);
CREATE INDEX idx_mfa_challenges_code ON mfa_challenges(code);
CREATE INDEX idx_mfa_challenges_expires_at ON mfa_challenges(expires_at);
