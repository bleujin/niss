@echo off

setlocal enabledelayedexpansion


:test
set /a "TESTPORT=%RANDOM%+3000"
netstat -an | findstr ":%TESTPORT% "
if %ERRORLEVEL%==0 goto test

rem for %%? in ("%~dp0..") do set HOMEDIR=%%~f?
set HOMEDIR=%cd%
IF not exist %JAVA_HOME%/jre (
	set JAVA_HOME=C:\java\jdk6_45
)
set JAVA_BIN=%JAVA_HOME%\bin\java
set CP=./;
set JAVA_ARGS=-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Djava.util.logging.config.file=%HOMEDIR%\resource\log4j.properties -Dsun.nio.ch.bugLevel="" 
set JMX_ARGS=-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=%TESTPORT% 
set GC_ARGS=-Xms512m -Xmx1024m -server
set PRG_ARGS=-config:%HOMEDIR%\resource\config\niss-config.xml

if not exist "%JAVA_HOME%\jre" goto no_java


@echo. running script for Niss Server

rem confirm setted vars 
@echo. == Settted Vars ==
@echo. HOMEDIR=%HOMEDIR%
@echo. JAVA_HOME=%JAVA_HOME%
@echo. CLASSPATH=%CP%
@echo. JAVA_ARGS=%JAVA_ARGS%
@echo. JMX_ARGS=%JMX_ARGS%
@echo. GC_ARGS=%GC_ARGS%
@echo. PRG_ARGS=%PRG_ARGS% %*

rem start java %GC_ARGS% %JMX_ARGS% %JAVA_ARGS% -jar niss_0.6.jar %PRG_ARGS% %*
start %JAVA_BIN% %GC_ARGS% %JMX_ARGS% %JAVA_ARGS% -jar niss_0.6.jar %PRG_ARGS% %*

goto end

:no_java
@echo. This install script requires the parameter to specify Java location
@echo. The Java run-time files tools.jar and jvm.dll must exist under that location
goto error_exit


:error_exit
@echo .
@echo . Failed to run AradonServer

:end
@echo.
@pause
