-- Script de setup des utilisateurs de test pour les tests de charge
-- Créé automatiquement pour supporter 100 utilisateurs avec portefeuilles

-- ===========================================
-- CRÉATION DES UTILISATEURS (auth-service)
-- ===========================================

-- Supprimer les utilisateurs de test existants (pour pouvoir relancer le script)
DELETE FROM Users WHERE email LIKE 'testuser%@loadtest.com';

-- Insérer 100 utilisateurs de test (IDs 1-100)
INSERT INTO Users (id, email, passwordHash, name, adresse, dateDeNaissance, status) VALUES
-- Utilisateurs 1-10
(1, 'testuser1@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 1', '123 Test St, Montreal', '1990-01-01', 'ACTIVE'),
(2, 'testuser2@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 2', '124 Test St, Montreal', '1990-01-02', 'ACTIVE'),
(3, 'testuser3@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 3', '125 Test St, Montreal', '1990-01-03', 'ACTIVE'),
(4, 'testuser4@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 4', '126 Test St, Montreal', '1990-01-04', 'ACTIVE'),
(5, 'testuser5@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 5', '127 Test St, Montreal', '1990-01-05', 'ACTIVE'),
(6, 'testuser6@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 6', '128 Test St, Montreal', '1990-01-06', 'ACTIVE'),
(7, 'testuser7@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 7', '129 Test St, Montreal', '1990-01-07', 'ACTIVE'),
(8, 'testuser8@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 8', '130 Test St, Montreal', '1990-01-08', 'ACTIVE'),
(9, 'testuser9@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 9', '131 Test St, Montreal', '1990-01-09', 'ACTIVE'),
(10, 'testuser10@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User 10', '132 Test St, Montreal', '1990-01-10', 'ACTIVE');

-- Génération automatique des utilisateurs 11-100
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 11..100 LOOP
        INSERT INTO Users (id, email, passwordHash, name, adresse, dateDeNaissance, status) VALUES
        (i, 'testuser' || i || '@loadtest.com', '$2a$10$dummy.hash.for.testing', 'Test User ' || i, (100 + i) || ' Test St, Montreal', '1990-01-' || LPAD(i::text, 2, '0'), 'ACTIVE');
    END LOOP;
END $$;

-- Réinitialiser la séquence d'auto-increment pour éviter les conflits
SELECT setval('users_id_seq', 100, true);

COMMIT;
