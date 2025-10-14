CREATE TABLE order_book (
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
    reject_reason VARCHAR(255),
    quantity_remaining INT NOT NULL,
    order_id BIGINT NOT NULL
);

CREATE INDEX idx_order_book_user_id ON order_book(user_id);
CREATE INDEX idx_order_book_client_order_id ON order_book(client_order_id);
CREATE INDEX idx_order_book_symbol ON order_book(symbol);
CREATE INDEX idx_order_book_order_id ON order_book(order_id);
