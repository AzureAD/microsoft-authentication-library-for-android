#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
emulator @test -no-window -no-audio -wipe-data &
sleep 30
gradle -version
echo =============================================
echo Running instrumented tests
echo =============================================
gradle msal:connectedLocalDebugAndroidTest Psugar=true -i

