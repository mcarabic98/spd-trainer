@echo off
rem ============================================================
rem  Сборка spd-trainer.jar для Shattered Pixel Dungeon v4.0.2
rem  Компилирует пакет trainer и патченные классы игры против
rem  официальной банки ShatteredPD-v4.0.2-Java.jar. Сама
rem  официальная банка при этом не изменяется.
rem ============================================================
setlocal
cd /d "%~dp0"

rem jar не всегда есть в PATH, ищем его в установленном JDK
set "JARTOOL=jar"
where jar >nul 2>nul
if not errorlevel 1 goto :jartool_found
set "JARTOOL=C:\Program Files\Java\jdk-25.0.2\bin\jar.exe"
if not exist "%JARTOOL%" (
    echo ERROR: jar tool not found in PATH or JDK folder.
    exit /b 1
)
:jartool_found

if exist build\trainer-classes rmdir /s /q build\trainer-classes
mkdir build\trainer-classes

echo [1/2] Compiling trainer + patched game classes...
javac --release 11 -encoding UTF-8 -cp ShatteredPD-v4.0.2-Java.jar -d build\trainer-classes ^
 src\main\java\com\shatteredpixel\shatteredpixeldungeon\trainer\*.java ^
 src\main\java\com\shatteredpixel\shatteredpixeldungeon\actors\Char.java ^
 src\main\java\com\shatteredpixel\shatteredpixeldungeon\windows\WndGame.java
if errorlevel 1 (
    echo COMPILE FAILED
    exit /b 1
)

echo [2/2] Packaging dist\spd-trainer.jar ...
if not exist dist mkdir dist
"%JARTOOL%" --create --file dist\spd-trainer.jar -C build\trainer-classes .
if errorlevel 1 (
    echo PACKAGING FAILED
    exit /b 1
)

echo Done: dist\spd-trainer.jar
endlocal
