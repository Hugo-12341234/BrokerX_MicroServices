CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    client_order_id VARCHAR(64) UNIQUE,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    side VARCHAR(16) NOT NULL,
    type VARCHAR(16) NOT NULL,
    quantity INT NOT NULL,
    price DOUBLE PRECISION,
    duration VARCHAR(16) NOT NULL,
    timestamp TIMESTAMP(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reject_reason VARCHAR(255)
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_client_order_id ON orders(client_order_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);
