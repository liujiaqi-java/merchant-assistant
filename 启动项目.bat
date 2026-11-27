@echo off
setlocal
cd /d "%~dp0"

docker info >nul 2>&1
if errorlevel 1 (
  if not exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    echo Docker Desktop was not found at the standard installation path.
    pause
    exit /b 1
  )
  echo Starting Docker Desktop. Please wait for the project to load...
  start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
  set /a attempts=0
  goto wait_for_docker
)

:docker_ready
echo Docker is ready.
goto start_project

:wait_for_docker
docker info >nul 2>&1
if not errorlevel 1 goto docker_ready
set /a attempts+=1
if %attempts% GEQ 90 (
  echo Docker did not become ready within 3 minutes. Check Docker Desktop.
  pause
  exit /b 1
)
ping -n 3 127.0.0.1 >nul
goto wait_for_docker

:start_project

docker compose up -d
if errorlevel 1 (
  echo Failed to start the project. Check Docker Desktop and try again.
  pause
  exit /b 1
)

echo Project is starting at http://127.0.0.1:5173/
ping -n 7 127.0.0.1 >nul
start "" "http://127.0.0.1:5173/"
endlocal
