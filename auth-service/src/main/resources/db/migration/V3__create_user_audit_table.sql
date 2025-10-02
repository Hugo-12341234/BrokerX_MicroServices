CREATE TABLE user_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    document_hash VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_token VARCHAR(512),
    CONSTRAINT fk_user_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_audit_user_id ON user_audit(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_user_audit_timestamp ON user_audit(timestamp);
CREATE INDEX idx_user_audit_ip_address ON user_audit(ip_address);
CREATE INDEX idx_user_audit_action ON user_audit(action);
