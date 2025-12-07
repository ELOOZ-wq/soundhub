@echo off
echo Importation de la base de données soundhub...
echo.

REM Essayez d'abord sans mot de passe
C:\MAMP\bin\mysql\bin\mysql.exe -u root --ssl-mode=DISABLED -e "CREATE DATABASE IF NOT EXISTS soundhub;" 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Base de données créée.
    C:\MAMP\bin\mysql\bin\mysql.exe -u root --ssl-mode=DISABLED soundhub < soundhub.sql
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✓ Base de données importée avec succès!
        pause
        exit /b 0
    )
)

REM Si ça échoue, essayez avec le mot de passe "root"
echo Tentative avec le mot de passe "root"...
C:\MAMP\bin\mysql\bin\mysql.exe -u root -proot --ssl-mode=DISABLED -e "CREATE DATABASE IF NOT EXISTS soundhub;" 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Base de données créée.
    C:\MAMP\bin\mysql\bin\mysql.exe -u root -proot --ssl-mode=DISABLED soundhub < soundhub.sql
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✓ Base de données importée avec succès!
        pause
        exit /b 0
    )
)

echo.
echo ✗ Erreur: Impossible d'importer la base de données.
echo.
echo Solutions:
echo 1. Vérifiez que MAMP est démarré
echo 2. Trouvez le mot de passe MySQL dans les préférences MAMP
echo 3. Modifiez ce script avec le bon mot de passe: mysql -u root -pVOTRE_MOT_DE_PASSE
echo 4. Ou importez manuellement via phpMyAdmin: http://localhost/phpMyAdmin
echo.
pause

