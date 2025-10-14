INSERT INTO order_book (client_order_id, user_id, symbol, side, type, quantity, price, duration, timestamp, status, reject_reason, quantity_remaining)
VALUES
    ('seed-AAPL', 9999, 'AAPL', 'VENTE', 'LIMITE', 5, 100, 'DAY', NOW(), 'Working', NULL, 5),
    ('seed-MSFT', 9999, 'MSFT', 'VENTE', 'LIMITE', 5, 200, 'DAY', NOW(), 'Working', NULL, 5),
    ('seed-TSLA', 9999, 'TSLA', 'VENTE', 'LIMITE', 10, 150, 'DAY', NOW(), 'Working', NULL, 10),
    ('seed-GOOG', 9999, 'GOOG', 'VENTE', 'LIMITE', 12, 1200, 'DAY', NOW(), 'Working', NULL, 12);

-- Mise à jour du champ order_id pour chaque ligne insérée
UPDATE order_book SET order_id = id WHERE user_id = 9999 AND client_order_id LIKE 'seed-%';
