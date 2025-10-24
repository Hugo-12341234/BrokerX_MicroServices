-- Script de nettoyage des utilisateurs de test après les tests de charge
-- Supprime tous les utilisateurs créés pour les tests

-- ===========================================
-- NETTOYAGE DES UTILISATEURS DE TEST
-- ===========================================

-- Supprimer tous les utilisateurs de test
DELETE FROM Users WHERE email LIKE 'testuser%@loadtest.com';

-- Afficher le résumé du nettoyage pour auth-service
SELECT
    'Utilisateurs supprimés' as action,
    'Tous les testuser1-100@loadtest.com' as details;

COMMIT;
