@echo off
echo ========================================
echo NETTOYAGE DES DONNEES DE TEST
echo ========================================
echo.
echo Ce script va supprimer TOUTES les donnees de test:
echo - 99 utilisateurs (testuser2-100@loadtest.com)
echo - Tous leurs portefeuilles
echo - Toutes leurs positions d'actions
echo.
echo ⚠️  ATTENTION: Cette action est IRREVERSIBLE!
echo.
set /p confirm="Voulez-vous vraiment supprimer toutes les donnees de test? (oui/non): "
if /i not "%confirm%"=="oui" (
    echo Operation annulee.
    pause
    exit /b 0
)

echo.
echo ========================================
echo SUPPRESSION EN COURS...
echo ========================================

REM Verification que Docker est lance
docker ps >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Docker n'est pas lance ou accessible
    echo Lancez d'abord: docker-compose up
    pause
    exit /b 1
)

echo.
echo 1. Nettoyage des portefeuilles et positions (Wallet Service)...
docker exec -i brokerx-postgres-wallet psql -U postgres -d brokerxdb_wallet < cleanup-test-wallets.sql

if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Echec du nettoyage des portefeuilles
    pause
    exit /b 1
)

echo ✅ Portefeuilles et positions supprimes!

echo.
echo 2. Nettoyage des utilisateurs (Auth Service)...
docker exec -i brokerx-postgres-auth psql -U postgres -d brokerxdb_auth < cleanup-test-users.sql

if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Echec du nettoyage des utilisateurs
    pause
    exit /b 1
)

echo ✅ Utilisateurs supprimes!

echo.
echo ========================================
echo VERIFICATION DU NETTOYAGE
echo ========================================

echo.
echo Verification - utilisateurs restants:
docker exec brokerx-postgres-auth psql -U postgres -d brokerxdb_auth -c "SELECT COUNT(*) as remaining_test_users FROM Users WHERE email LIKE 'testuser%%@loadtest.com';"

echo.
echo Verification - portefeuilles restants:
docker exec brokerx-postgres-wallet psql -U postgres -d brokerxdb_wallet -c "SELECT COUNT(*) as remaining_test_wallets FROM wallet WHERE user_id BETWEEN 2 AND 100;"

echo.
echo ========================================
echo NETTOYAGE TERMINE!
echo ========================================
echo.
echo ✅ Toutes les donnees de test ont ete supprimees
echo ✅ Votre base de donnees est maintenant propre
echo.
echo Pour relancer des tests de charge:
echo    1. setup-load-test-data.bat (recreation des donnees)
echo    2. run-load-test.bat (execution du test)
echo.
pause
