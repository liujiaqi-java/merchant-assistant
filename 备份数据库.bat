@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\backup-database.ps1"
if errorlevel 1 (
  echo Database backup failed.
  pause
  exit /b 1
)
pause
endlocal
