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

echo JDK не найден. Установите JDK 17+.
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

echo Тест 1: Реальный репозиторий (curl), глубина 2
java -cp out ru.miem.depgraph.Main --package curl --repo "%CD%\repo.txt" --mode local --depth 2
echo.

echo Тест 2: Тестовый граф (A), глубина 3 (ограничение глубины)
java -cp out ru.miem.depgraph.Main --package A --repo "%CD%\test_graph.txt" --mode test --depth 3
echo.

echo Тест 3: Тестовый граф (A), глубина 5 (обнаружение цикла)
java -cp out ru.miem.depgraph.Main --package A --repo "%CD%\test_graph.txt" --mode test --depth 5
echo.

echo Тест 4: Тестовый граф (X), глубина 10 (изолированный цикл X-Y)
java -cp out ru.miem.depgraph.Main --package X --repo "%CD%\test_graph.txt" --mode test --depth 10
echo.

pause