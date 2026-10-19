@echo off
chcp 65001 >nul
cd /d "%~dp0"

set "JAVA_HOME="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto :jdk_found
if exist "%ProgramFiles%\Java\jdk-17\bin\javac.exe" set "JAVA_HOME=%ProgramFiles%\Java\jdk-17"

for /d %%i in ("%USERPROFILE%\.jdks\*") do (
    if exist "%%i\bin\javac.exe" set "JAVA_HOME=%%i"
)
if defined JAVA_HOME goto :jdk_found

for /d %%i in ("C:\Program Files\Java\jdk*") do (
    if exist "%%i\bin\javac.exe" set "JAVA_HOME=%%i"
)
if defined JAVA_HOME goto :jdk_found

for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
    if exist "%%i\bin\javac.exe" set "JAVA_HOME=%%i"
)
if defined JAVA_HOME goto :jdk_found

echo JDK не найден. Установите JDK 17+ и повторите попытку.
pause
exit /b 1

:jdk_found
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Используется JDK: %JAVA_HOME%
echo.

if not exist out mkdir out
javac -encoding UTF-8 -d out src\ru\miem\depgraph\Main.java
if errorlevel 1 (
    echo Ошибка сборки!
    pause
    exit /b 1
)

echo Пример 1: Успешный поиск зависимостей для curl 7.81.0
java -cp out ru.miem.depgraph.Main --package curl --repo "%CD%\repo.txt" --mode local --version 7.81.0
echo.

echo Пример 2: Поиск зависимостей для bash 5.1.0
java -cp out ru.miem.depgraph.Main --package bash --repo "%CD%\repo.txt" --mode local --version 5.1.0
echo.

echo Пример 3: Ошибка (пакет не найден)
java -cp out ru.miem.depgraph.Main --package unknown-pkg --repo "%CD%\repo.txt" --mode local
echo.

pause