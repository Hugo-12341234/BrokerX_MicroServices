-- Script de nettoyage des données de test après les tests de charge
-- Supprime tous les utilisateurs, portefeuilles et positions créés pour les tests

-- ===========================================
-- NETTOYAGE DES DONNÉES DE TEST
-- ===========================================

-- Supprimer toutes les positions d'actions des utilisateurs de test
DELETE FROM stock_position
WHERE wallet_id IN (
    SELECT id FROM wallet WHERE user_id BETWEEN 2 AND 100
);

-- Supprimer tous les portefeuilles des utilisateurs de test
DELETE FROM wallet WHERE user_id BETWEEN 2 AND 100;

-- Afficher le résumé du nettoyage pour wallet-service
SELECT
    'Positions supprimées' as action,
    'Toutes les positions des utilisateurs 2-100' as details
UNION ALL
SELECT
    'Portefeuilles supprimés' as action,
    'Tous les portefeuilles des utilisateurs 2-100' as details;

COMMIT;
