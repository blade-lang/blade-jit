@echo off

set "DIR=%~dp0"

rem Collect all program arguments passed to this batch file.
set "PROGRAM_ARGS="
:loop
if "%1"=="" goto end
set "PROGRAM_ARGS=%PROGRAM_ARGS% "%1""
shift
goto loop
:end

set "JAVA_ARGS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"

if not exist "%JAVA_HOME%\lib\graalvm\" (
    echo.
    echo Warning: Could not find GraalVM on %JAVA_HOME%. Running on JDK without JIT support.
    echo.
)

"%JAVA_HOME%\bin\java" %JAVA_ARGS% -cp "bin;%DIR%build\modules\*" org.blade.Main %PROGRAM_ARGS%
