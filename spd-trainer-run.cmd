@echo off
cd /d "%~dp0"
java -DSpecification-Title="Shattered Pixel Dungeon" -DSpecification-Version=4.0.2 -DImplementation-Title=com.shatteredpixel.shatteredpixeldungeon -DImplementation-Version=922 --enable-native-access=ALL-UNNAMED -cp "dist\spd-trainer.jar;ShatteredPD-v4.0.2-Java.jar" com.shatteredpixel.shatteredpixeldungeon.desktop.DesktopLauncher > trainer.log 2>&1
