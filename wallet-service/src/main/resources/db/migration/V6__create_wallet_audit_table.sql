CREATE TABLE wallet_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    details VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wallet_audit_user_id ON wallet_audit(user_id);
