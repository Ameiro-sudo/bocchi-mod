@echo off
rem Bocchi Designer launcher: serve locally then open browser.
rem Close the minimized server window to stop. ASCII only (cmd codepage safe).
setlocal
cd /d "%~dp0"

powershell -NoProfile -Command "try{ $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8833/' -TimeoutSec 1; if($r.StatusCode -eq 200 -and $r.Content -match 'Bocchi'){ exit 0 } }catch{}; exit 1" >nul 2>nul
if %errorlevel%==0 goto open

where py >nul 2>nul
if %errorlevel%==0 (
  start "Bocchi Designer Server :8833" /min cmd /c "py -m http.server 8833 --bind 127.0.0.1"
  goto waitopen
)
where python >nul 2>nul
if %errorlevel%==0 (
  start "Bocchi Designer Server :8833" /min cmd /c "python -m http.server 8833 --bind 127.0.0.1"
  goto waitopen
)
echo [ERROR] Python 3 not found in PATH. Install Python or serve this folder
echo         with any static file server, then open http://127.0.0.1:8833/
pause
exit /b 1

:waitopen
timeout /t 1 /nobreak >nul

:open
start "" "http://127.0.0.1:8833/"
endlocal
exit /b 0
