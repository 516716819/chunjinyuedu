@echo off
rem legado POC 构建脚本（Windows）—— 不依赖、也不修改系统环境变量
rem 用法：双击本文件，或在 cmd 里执行 build.cmd [gradle 任务]
setlocal

set "JAVA_HOME=D:\Work\JAVA\jdk21"
cd /d "%~dp0"

if "%~1"=="" (
    set "TASKS=assembleDebug"
) else (
    set "TASKS=%*"
)

echo [build.cmd] JAVA_HOME=%JAVA_HOME%
echo [build.cmd] gradle -g D:/Work/gradle-cache %TASKS%
call gradlew.bat -g "D:/Work/gradle-cache" %TASKS%
