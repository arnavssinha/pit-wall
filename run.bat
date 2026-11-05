@echo off
rem Runs the prebuilt PitWall.jar on Windows.
cd /d "%~dp0"
java -jar PitWall.jar %*
