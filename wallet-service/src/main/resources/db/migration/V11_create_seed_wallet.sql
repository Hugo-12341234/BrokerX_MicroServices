INSERT INTO wallet (id, user_id, balance, created_at, updated_at) VALUES (gen_random_uuid(), 9999, 1000000, NOW(), NOW());

-- Pour chaque symbole
INSERT INTO stock_position (id, wallet_id, symbol, quantity, average_price, created_at, updated_at)
SELECT gen_random_uuid(), w.id, s, 1000, 0, NOW(), NOW()
FROM wallet w, (VALUES ('AAPL'), ('MSFT'), ('TSLA'), ('GOOG')) AS symbols(s)
WHERE w.user_id = 9999;
