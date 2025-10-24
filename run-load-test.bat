@echo off
echo ========================================
echo Installation et execution du test k6
echo ========================================

REM Verification si k6 est deja installe
k6 version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo k6 n'est pas installe. Installation en cours...

    REM Installation via chocolatey (si disponible)
    where choco >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo Installation via Chocolatey...
        choco install k6 -y
    ) else (
        echo.
        echo ATTENTION: k6 n'est pas installe et chocolatey n'est pas disponible.
        echo.
        echo Veuillez installer k6 manuellement:
        echo 1. Telecharger depuis: https://k6.io/docs/get-started/installation/
        echo 2. Ou installer chocolatey puis relancer ce script
        echo 3. Ou utiliser: winget install k6
        echo.
        pause
        exit /b 1
    )
) else (
    echo k6 est deja installe ✓
)

echo.
echo ========================================
echo Lancement du test de charge
echo Objectif: 800+ ordres/seconde
echo ========================================
echo.

REM Verification que les services sont demarres
echo Verification des services...
ping -n 1 localhost >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Impossible de joindre localhost
    echo Assurez-vous que Docker Compose est lance: docker-compose up
    pause
    exit /b 1
)

echo Services accessibles ✓
echo.
echo ========================================
echo DASHBOARDS DE MONITORING
echo ========================================
echo.
echo Pendant le test, surveillez vos dashboards:
echo - Grafana: http://localhost:3000
echo - Prometheus: http://localhost:9090
echo.
echo Le test va durer environ 10 minutes total.
echo Observez particulierement:
echo - CPU/Memoire des microservices
echo - Latence des APIs
echo - Taux d'erreur
echo - Throughput des ordres
echo.
echo ========================================
echo Demarrage du test k6...
echo.
echo ⚠️  CONTROLE DU TEST:
echo    • Ctrl+C = Arrêt immédiat à tout moment
echo    • Le test sauvegarde les résultats même si interrompu
echo    • Durée totale prévue: ~10 minutes
echo.
echo 🔧 AUCUNE MODIFICATION de votre code nécessaire!
echo    k6 fait simplement des appels HTTP vers vos APIs
echo.
echo ========================================
echo.

REM Execution du test avec options de sortie - CORRIGÉ pour supprimer InfluxDB
k6 run --out json=test-results.json load-test-orders.js

echo.
echo ========================================
echo Test termine!
echo ========================================
echo.
echo Resultats sauvegardes:
echo - Fichier JSON: %CD%\test-results.json
echo - Dashboards Grafana: http://localhost:3000
echo.
echo Analysez maintenant:
echo 1. Les pics de CPU/memoire pendant la montee en charge
echo 2. Le point de rupture (quand les erreurs augmentent)
echo 3. La latence moyenne vs pic de charge
echo 4. Les goulots d'etranglement identifies
echo.

echo Pour analyser les resultats:
echo - Grafana: http://localhost:3000 (si configure)
echo - Prometheus: http://localhost:9090 (si configure)
echo.
pause
