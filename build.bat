@echo off
rem Builds Pit Wall on Windows: compiles src\*.java and packs PitWall.jar.
cd /d "%~dp0"
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
javac --release 11 -Xlint:all,-serial -Werror -encoding UTF-8 -cp lib\acm.jar -d build\classes src\*.java
if errorlevel 1 exit /b 1
xcopy /e /i /q data build\classes\data >nul
(echo Main-Class: PitWall& echo Class-Path: lib/acm.jar) > build\manifest.txt
jar cfm PitWall.jar build\manifest.txt -C build\classes .
echo Built PitWall.jar
