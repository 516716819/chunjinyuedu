#!/usr/bin/env bash
# legado POC 构建脚本 —— 不依赖、也不修改任何系统环境变量
#
# 三件事都封装在这里：
#   1. JAVA_HOME 指向 jdk21（legado 要求 Java 21，系统变量里的 JDK 8 不受影响）
#   2. -g 指向 D 盘缓存联接（规避 KSP 的跨盘符路径 bug）
#   3. 默认任务是 assembleDebug，可传任意 gradle 参数覆盖
#
# 用法：
#   ./build.sh                     # 编 debug 包
#   ./build.sh clean assembleDebug # 透传参数
set -e

export JAVA_HOME="/d/Work/JAVA/jdk21"
cd "$(dirname "$0")"

TASKS="${*:-assembleDebug}"
echo "[build.sh] JAVA_HOME=$JAVA_HOME"
echo "[build.sh] gradle -g D:/Work/gradle-cache $TASKS"
exec ./gradlew -g "D:/Work/gradle-cache" $TASKS
