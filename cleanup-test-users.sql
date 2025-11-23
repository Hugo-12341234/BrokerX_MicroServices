-- Script de nettoyage des utilisateurs de test après les tests de charge
-- Supprime les 99 utilisateurs créés pour les tests (IDs 2-100)

-- ===========================================
-- NETTOYAGE DES UTILISATEURS DE TEST
-- ===========================================

-- Supprimer tous les utilisateurs de test
DELETE FROM Users WHERE email LIKE 'testuser%@loadtest.com';

-- Afficher le résumé du nettoyage pour auth-service
SELECT
    'Utilisateurs supprimés' as action,
    'Tous les testuser2-100@loadtest.com' as details;

COMMIT;
