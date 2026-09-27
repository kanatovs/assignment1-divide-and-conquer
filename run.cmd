@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
	echo ERROR: javac was not found. Install or select a JDK 17+ and add its bin folder to PATH.
	exit /b 1
)
if not exist out mkdir out
javac --release 17 -encoding UTF-8 -d out src\*.java tests\*.java
if errorlevel 1 exit /b 1
set "TASK=%~1"
if "%TASK%"=="" set "TASK=all"
if /I "%TASK%"=="test" (
	java -Djava.awt.headless=true -cp out AllTests
	exit /b
)
if /I "%TASK%"=="all" (
	java -Djava.awt.headless=true -cp out AllTests
	if errorlevel 1 exit /b 1
)
java -Djava.awt.headless=true -cp out Main %TASK%
exit /b
