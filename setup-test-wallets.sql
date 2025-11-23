-- Script de setup des portefeuilles et positions pour les tests de charge
-- Créé automatiquement pour supporter 100 utilisateurs avec portefeuilles bien garnis

-- ===========================================
-- CRÉATION DES PORTEFEUILLES (wallet-service)
-- ===========================================

-- Supprimer les portefeuilles de test existants (pour pouvoir relancer le script)
DELETE FROM stock_position WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id BETWEEN 2 AND 100);
DELETE FROM wallet WHERE user_id BETWEEN 2 AND 100;

-- Insérer les portefeuilles pour les utilisateurs 2-100 (99 utilisateurs)
-- Chaque utilisateur aura 50,000$ en cash pour commencer
DO $$
DECLARE
    i INTEGER;
    wallet_uuid UUID;
BEGIN
    FOR i IN 2..100 LOOP
        -- Générer un UUID pour le portefeuille
        wallet_uuid := gen_random_uuid();

        -- Créer le portefeuille avec 50,000$ de balance
        INSERT INTO wallet (id, user_id, balance, created_at, updated_at) VALUES
        (wallet_uuid, i, 50000.00, NOW(), NOW());

        -- Ajouter des positions d'actions diversifiées pour chaque utilisateur
        -- AAPL: 200 actions à 150$ chacune (quantité augmentée)
        INSERT INTO stock_position (id, wallet_id, symbol, quantity, average_price, created_at, updated_at) VALUES
        (gen_random_uuid(), wallet_uuid, 'AAPL', 200, 150.00, NOW(), NOW());

        -- MSFT: 150 actions à 300$ chacune (quantité augmentée)
        INSERT INTO stock_position (id, wallet_id, symbol, quantity, average_price, created_at, updated_at) VALUES
        (gen_random_uuid(), wallet_uuid, 'MSFT', 150, 300.00, NOW(), NOW());

        -- TSLA: 200 actions à 200$ chacune (quantité augmentée)
        INSERT INTO stock_position (id, wallet_id, symbol, quantity, average_price, created_at, updated_at) VALUES
        (gen_random_uuid(), wallet_uuid, 'TSLA', 200, 200.00, NOW(), NOW());

    END LOOP;
END $$;

COMMIT;

-- ===========================================
-- VÉRIFICATION DES DONNÉES CRÉÉES
-- ===========================================

-- Afficher un résumé des données créées
SELECT
    'Utilisateurs créés' as type,
    COUNT(*) as count
FROM Users
WHERE email LIKE 'testuser%@loadtest.com'

UNION ALL

SELECT
    'Portefeuilles créés' as type,
    COUNT(*) as count
FROM wallet
WHERE user_id BETWEEN 1 AND 100

UNION ALL

SELECT
    'Positions d''actions créées' as type,
    COUNT(*) as count
FROM stock_position sp
JOIN wallet w ON sp.wallet_id = w.id
WHERE w.user_id BETWEEN 1 AND 100

UNION ALL

SELECT
    'Balance totale (cash)' as type,
    SUM(balance)::INTEGER as count
FROM wallet
WHERE user_id BETWEEN 1 AND 100;

-- Afficher un exemple de portefeuille (utilisateur 1)
SELECT
    w.user_id,
    w.balance as cash_balance,
    sp.symbol,
    sp.quantity,
    sp.average_price,
    (sp.quantity * sp.average_price) as position_value
FROM wallet w
LEFT JOIN stock_position sp ON w.id = sp.wallet_id
WHERE w.user_id = 1
ORDER BY sp.symbol;
