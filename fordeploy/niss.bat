@echo off

:start
set HOMEDIR=%cd%
set JAVA_GC_ARGS=-Xms512m -Xmx1024m -server
set PRG_ARGS=-config:%HOMEDIR%\resource\config\niss-config.xml

CALL common.bat
