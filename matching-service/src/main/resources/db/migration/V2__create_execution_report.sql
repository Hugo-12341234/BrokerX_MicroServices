CREATE TABLE execution_report (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES order_book(id) ON DELETE CASCADE,
    fill_quantity INT NOT NULL,
    fill_price DECIMAL(18,4) NOT NULL,
    fill_type VARCHAR(16) NOT NULL,
    execution_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

