@echo off
echo ========================================
echo SETUP DES UTILISATEURS DE TEST
echo ========================================
echo.
echo Ce script va creer 100 utilisateurs de test avec:
echo - Comptes utilisateurs dans auth-service
echo - Portefeuilles avec 50,000$ chacun
echo - Positions diversifiees dans 8 actions
echo.

REM Verification que Docker est lance
docker ps >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Docker n'est pas lance ou accessible
    echo Lancez d'abord: docker-compose up
    pause
    exit /b 1
)

echo ========================================
echo 1. CREATION DES UTILISATEURS (Auth Service)
echo ========================================
echo.
echo Connexion a la base auth-service...
docker exec -i brokerx-postgres-auth psql -U postgres -d brokerxdb_auth < setup-test-users.sql

if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Echec de creation des utilisateurs
    pause
    exit /b 1
)

echo ✅ Utilisateurs crees avec succes!
echo.

echo ========================================
echo 2. CREATION DES PORTEFEUILLES (Wallet Service)
echo ========================================
echo.
echo Connexion a la base wallet-service...
docker exec -i brokerx-postgres-wallet psql -U postgres -d brokerxdb_wallet < setup-test-wallets.sql

if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Echec de creation des portefeuilles
    pause
    exit /b 1
)

echo ✅ Portefeuilles et positions crees avec succes!
echo.

echo ========================================
echo VERIFICATION DES DONNEES CREEES
echo ========================================
echo.

echo Verification des utilisateurs:
docker exec brokerx-postgres-auth psql -U postgres -d brokerxdb_auth -c "SELECT COUNT(*) as users_created FROM Users WHERE email LIKE 'testuser%%@loadtest.com';"

echo.
echo Verification des portefeuilles:
docker exec brokerx-postgres-wallet psql -U postgres -d brokerxdb_wallet -c "SELECT COUNT(*) as wallets_created FROM wallet WHERE user_id BETWEEN 2 AND 100;"

echo.
echo Verification des positions d'actions:
docker exec brokerx-postgres-wallet psql -U postgres -d brokerxdb_wallet -c "SELECT COUNT(*) as positions_created FROM stock_position sp JOIN wallet w ON sp.wallet_id = w.id WHERE w.user_id BETWEEN 2 AND 100;"

echo.
echo Exemple de portefeuille (utilisateur 2):
docker exec brokerx-postgres-wallet psql -U postgres -d brokerxdb_wallet -c "SELECT w.user_id, w.balance as cash, sp.symbol, sp.quantity, sp.average_price FROM wallet w LEFT JOIN stock_position sp ON w.id = sp.wallet_id WHERE w.user_id = 2 ORDER BY sp.symbol;"

echo.
echo ========================================
echo SETUP TERMINE!
echo ========================================
echo.
echo ✅ 99 utilisateurs de test sont maintenant prets
echo ✅ Chaque utilisateur a:
echo    - 50,000$ en cash
echo    - Positions dans 8 actions differentes
echo    - Total: ~500,000$ de valeur par portefeuille
echo.
echo Vous pouvez maintenant lancer le test de charge:
echo    run-load-test.bat
echo.
pause
