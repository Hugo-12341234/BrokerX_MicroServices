@echo off
REM Script de test Load Balancing pour Windows
REM Teste différents nombres d'instances avec NGINX Load Balancer

echo ================================
echo BrokerX Load Balancing Test
echo ================================

echo.
echo Choisissez une option:
echo 1. Demarrer avec 1 instance par service
echo 2. Demarrer avec 2 instances par service
echo 3. Demarrer avec 3 instances par service
echo 4. Demarrer avec 4 instances par service
echo 5. Test de tolerance aux pannes
echo 6. Afficher le status des containers
echo 7. Arreter tous les services

set /p choice="Votre choix (1-7): "

if "%choice%"=="1" goto start_1
if "%choice%"=="2" goto start_2
if "%choice%"=="3" goto start_3
if "%choice%"=="4" goto start_4
if "%choice%"=="5" goto test_fault
if "%choice%"=="6" goto show_status
if "%choice%"=="7" goto stop_all

:start_1
echo.
echo Demarrage avec 1 instance par service...
docker-compose down
docker-compose up -d
goto show_urls

:start_2
echo.
echo Demarrage avec 2 instances par service...
docker-compose down
docker-compose up -d --scale auth-service=2 --scale order-service=2 --scale wallet-service=2 --scale matching-service=2
goto show_urls

:start_3
echo.
echo Demarrage avec 3 instances par service...
docker-compose down
docker-compose up -d --scale auth-service=3 --scale order-service=3 --scale wallet-service=3 --scale matching-service=3
goto show_urls

:start_4
echo.
echo Demarrage avec 4 instances par service...
docker-compose down
docker-compose up -d --scale auth-service=4 --scale order-service=4 --scale wallet-service=4 --scale matching-service=4
goto show_urls

:test_fault
echo.
echo Test de tolerance aux pannes...
echo Arret d'une instance de wallet-service...
for /f "tokens=*" %%i in ('docker ps --filter "name=wallet-service" --format "{{.Names}}" ^| findstr /R ".*"') do (
    echo Arret de %%i
    docker stop %%i
    goto test_continue
)
:test_continue
echo.
echo Test du service apres panne...
timeout /t 3 /nobreak >nul
curl -s -o nul -w "Status: %%{http_code}" http://localhost:8079/actuator/health
echo.
echo Redemarrage de l'instance...
docker-compose up -d
goto end

:show_status
echo.
echo Status des containers:
echo =====================
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.
echo Instances par service:
for /f "tokens=*" %%i in ('docker ps --filter "name=auth-service" --format "{{.Names}}"') do echo Auth: %%i
for /f "tokens=*" %%i in ('docker ps --filter "name=order-service" --format "{{.Names}}"') do echo Order: %%i
for /f "tokens=*" %%i in ('docker ps --filter "name=wallet-service" --format "{{.Names}}"') do echo Wallet: %%i
for /f "tokens=*" %%i in ('docker ps --filter "name=matching-service" --format "{{.Names}}"') do echo Matching: %%i
goto end

:stop_all
echo.
echo Arret de tous les services...
docker-compose down
echo Services arretes.
goto end

:show_urls
echo.
echo ================================
echo Services demarres avec succes!
echo ================================
echo Frontend: http://localhost:3000
echo API Gateway: http://localhost:8079
echo Grafana: http://localhost:3001 (admin/admin)
echo Prometheus: http://localhost:8090
echo.
echo L'API Gateway route maintenant via NGINX Load Balancer
echo vers les instances multiples de microservices.
echo.

:end
pause
