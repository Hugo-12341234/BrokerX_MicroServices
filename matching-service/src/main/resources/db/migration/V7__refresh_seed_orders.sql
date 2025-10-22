-- Refresh des ordres seed existants pour les rendre "frais"
-- Cette migration met à jour les timestamps pour aujourd'hui
UPDATE order_book
SET
    timestamp = NOW(),
    status = 'Working'
WHERE client_order_id LIKE 'seed-%';

