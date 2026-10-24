@echo off
rem ============================================================
rem  Запуск официальной Shattered Pixel Dungeon v4.0.2
rem  с трейнером (spd-trainer.jar первым в classpath).
rem  Сам запуск — в spd-trainer-run.cmd (одна строка, без
rem  переносов). Вывод игры и трейнера пишется в trainer.log.
rem ============================================================
cd /d "%~dp0"
start /MIN "SPD Trainer" "%~dp0spd-trainer-run.cmd"
