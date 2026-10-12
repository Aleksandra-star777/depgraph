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

echo Пример успешного запуска:
java -cp out ru.miem.depgraph.Main --package requests --repo "%CD%\README.md" --mode local --version 2.31.0 --output deps.png --depth 5
echo.

echo Пример с ошибкой (несуществующий репозиторий):
java -cp out ru.miem.depgraph.Main --package requests --repo "./missing.json" --mode local
echo.

echo Пример с ошибкой (неверная глубина):
java -cp out ru.miem.depgraph.Main --package requests --repo "%CD%\README.md" --mode local --depth abc
echo.

echo Пример с ошибкой (неизвестный режим):
java -cp out ru.miem.depgraph.Main --package requests --repo "%CD%\README.md" --mode unknown
echo.

pause