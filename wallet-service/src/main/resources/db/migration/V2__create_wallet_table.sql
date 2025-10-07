CREATE TABLE walletEntitiy (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Index pour accès rapide par utilisateur
CREATE UNIQUE INDEX idx_wallet_user_id ON walletEntitiy(user_id);

