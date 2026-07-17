@echo off
setlocal

set "ROOT_DIR=%~dp0.."
set "JAR_PATH=%ROOT_DIR%\target\queuectl.jar"

java -jar "%JAR_PATH%" %*
exit /b %ERRORLEVEL%
