@echo off
setlocal
set APP_HOME=%~dp0
if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java.exe
)
"%JAVACMD%" -jar "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" %*
endlocal
