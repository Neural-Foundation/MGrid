@echo off

rem $Id: p-grid.bat,v 1.3 2005/11/18 15:21:02 rschmidt Exp $

if not "%PGRIDHOME%" == "" goto EXEC

cd %PGRIDHOME%

:EXEC

java -jar p-grid.jar %1 %2 %3 %4 %5 %6 %7 %8 %9

