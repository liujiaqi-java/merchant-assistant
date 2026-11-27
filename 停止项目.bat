@echo off
setlocal
cd /d "%~dp0"

docker info >nul 2>&1
if errorlevel 1 (
  echo Docker Desktop is not running. Nothing was stopped.
  pause
  exit /b 1
)

docker compose stop
if errorlevel 1 (
  echo Failed to stop the project. Check Docker Desktop and try again.
  pause
  exit /b 1
)

echo Project stopped. Data was kept.
pause
endlocal
