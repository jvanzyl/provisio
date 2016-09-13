@echo off
for %%X in (python.exe) do (set FOUND=%%~$PATH:X)
if not defined FOUND (
  echo No python.exe on PATH
  goto exit
)
python.exe %~dp0\launcher_win.py %*

:exit
