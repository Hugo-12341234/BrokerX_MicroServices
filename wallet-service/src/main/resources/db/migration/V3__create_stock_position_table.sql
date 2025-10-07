CREATE TABLE stock_position (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    quantity INT NOT NULL,
    average_price DECIMAL(18,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES walletEntitiy(id) ON DELETE CASCADE
);
CREATE INDEX idx_stock_position_wallet_id ON stock_position(wallet_id);
CREATE INDEX idx_stock_position_symbol ON stock_position(symbol);

