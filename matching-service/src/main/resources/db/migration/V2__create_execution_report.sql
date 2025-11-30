CREATE TABLE execution_report (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    fill_quantity INT NOT NULL,
    fill_price DECIMAL(18,4) NOT NULL,
    fill_type VARCHAR(16) NOT NULL,
    execution_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    symbol VARCHAR(32) NOT NULL
);

