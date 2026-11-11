@echo off
REM ============================================================
REM  BarberFlow - arranca a API local para a app (flavor "local")
REM  Requisitos: MySQL/MariaDB do XAMPP ligado (painel do XAMPP)
REM  A app no emulador chega aqui por http://10.0.2.2:8080
REM  Deixa esta janela aberta enquanto testas a app.
REM ============================================================
title BarberFlow API local (porta 8080)
echo A API fica em: http://127.0.0.1:8080/projeto/projeto/api/v1/index.php?r=barbearias/rlbarber
echo (Ctrl+C para parar)
echo.
C:\xampp\php\php.exe -S 127.0.0.1:8080 -t C:\xampp\htdocs
pause
