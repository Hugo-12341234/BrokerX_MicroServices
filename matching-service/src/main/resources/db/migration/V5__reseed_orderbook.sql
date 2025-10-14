INSERT INTO order_book (id, client_order_id, user_id, symbol, side, type, quantity, price, duration, timestamp, status, reject_reason, quantity_remaining, order_id)
VALUES
    (nextval('order_book_id_seq'), 'seed-AAPL', 9999, 'AAPL', 'VENTE', 'LIMITE', 5, 100, 'DAY', NOW(), 'Working', NULL, 5, currval('order_book_id_seq')),
    (nextval('order_book_id_seq'), 'seed-MSFT', 9999, 'MSFT', 'VENTE', 'LIMITE', 5, 200, 'DAY', NOW(), 'Working', NULL, 5, currval('order_book_id_seq')),
    (nextval('order_book_id_seq'), 'seed-TSLA', 9999, 'TSLA', 'VENTE', 'LIMITE', 10, 150, 'DAY', NOW(), 'Working', NULL, 5, currval('order_book_id_seq')),
    (nextval('order_book_id_seq'), 'seed-GOOG', 9999, 'GOOG', 'VENTE', 'LIMITE', 12, 1200, 'DAY', NOW(), 'Working', NULL, 5, currval('order_book_id_seq'));
