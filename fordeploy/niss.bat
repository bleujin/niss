@echo off

:start
set HOMEDIR=%cd%
set JAVA_GC_ARGS=-Xms512m -Xmx1024m -server
rem set PRG_ARGS=-config:%HOMEDIR%\resource\config\niss-config.xml
set PRG_ARGS=-config:.\resource\config\niss-config.xml

CALL common.bat
