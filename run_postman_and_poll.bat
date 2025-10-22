@echo off
REM Script para ejecutar la colección Postman con Newman y luego ejecutar el polling de estado usando PowerShell.
SETLOCAL ENABLEDELAYEDEXPANSION

set COLLECTION=%~dp0postman_collection.json
set ENV=%~dp0postman_environment.json
set EXPORT_ENV=%~dp0exported_env.json
set REPORT_HTML=%~dp0newman-report.html

necho Ejecutando Newman (asegúrate de tener Node.js y newman instalados)...
newman run "%COLLECTION%" -e "%ENV%" --export-environment "%EXPORT_ENV%" --reporters cli,html --reporter-html-export "%REPORT_HTML%" --delay-request 1000
if ERRORLEVEL 1 (
  echo Newman terminó con errores (exit code %ERRORLEVEL%). Revisa la salida arriba.
) else (
  echo Newman completado con éxito.
)

necho Llamando al script de polling en PowerShell para comprobar el estado del reporte...
powershell -ExecutionPolicy Bypass -File "%~dp0run_postman_and_poll.ps1" "%EXPORT_ENV%"

necho Fin.
ENDLOCAL
pause
it co