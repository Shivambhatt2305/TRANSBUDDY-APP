@echo off
cd /d "%~dp0"
setlocal enabledelayedexpansion
title TransBuddy - Mobile Installer & Launcher

echo =====================================================================
echo           TRANSBUDDY APP - AUTOMATED MOBILE DEPLOYMENT
echo =====================================================================
echo.

:: 1. Locate ADB executable
set "ADB_EXE="

if exist "C:\Users\ASUS\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_EXE=C:\Users\ASUS\AppData\Local\Android\Sdk\platform-tools\adb.exe"
) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else if exist "C:\Android\sdk\platform-tools\adb.exe" (
    set "ADB_EXE=C:\Android\sdk\platform-tools\adb.exe"
) else (
    where adb >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        set "ADB_EXE=adb"
    )
)

if "%ADB_EXE%"=="" (
    echo [ERROR] Could not find Android Debug Bridge (adb.exe).
    echo Please ensure Android Studio SDK / platform-tools are installed at:
    echo C:\Users\ASUS\AppData\Local\Android\Sdk\platform-tools\adb.exe
    echo.
    pause
    exit /b 1
)

echo [1/4] ADB Tool Found: "%ADB_EXE%"
echo.

:: 2. Check for connected device
:CHECK_DEVICE
echo [2/4] Detecting connected phone / emulator...
"%ADB_EXE%" devices > "%TEMP%\tb_devices.txt"
type "%TEMP%\tb_devices.txt"

findstr /v "List of devices attached" "%TEMP%\tb_devices.txt" | findstr "device" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo =====================================================================
    echo [WARNING] No authorized Android device detected!
    echo.
    echo Please check:
    echo   1. Your phone is connected via USB cable.
    echo   2. Unlock your phone screen.
    echo   3. When prompted, tap "Always allow from this computer" (USB Debugging).
    echo   4. Ensure USB Mode is set to "File Transfer (MTP)" in notifications.
    echo =====================================================================
    echo.
    echo Press any key to retry device detection, or close this window...
    pause >nul
    echo.
    goto CHECK_DEVICE
)
echo [OK] Authorized Android device connected!
echo.

:: 3. Check for APK file (build if missing)
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
echo [3/4] Preparing TransBuddy APK...

if not exist "%APK_PATH%" (
    echo APK not found. Compiling latest debug APK with Gradle...
    if exist "gradlew.bat" (
        call gradlew.bat assembleDebug --no-daemon
        if !ERRORLEVEL! NEQ 0 (
            echo.
            echo [ERROR] Gradle build failed! Check errors above.
            pause
            exit /b !ERRORLEVEL!
        )
    )
) else (
    echo Found existing build: "%APK_PATH%"
)
echo.

:: 4. Install APK onto connected device
echo [4/4] Installing TransBuddy onto your phone...
"%ADB_EXE%" install -r -d "%APK_PATH%"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo =====================================================================
    echo [ERROR] Installation failed!
    echo.
    echo Attempting clean reinstall...
    "%ADB_EXE%" uninstall com.transbuddy.app >nul 2>&1
    "%ADB_EXE%" install -r "%APK_PATH%"
    if !ERRORLEVEL! NEQ 0 (
        echo [ERROR] Re-installation failed. Please unlock your phone and check USB debugging.
        pause
        exit /b 1
    )
)

:: 5. Grant Camera & Location permissions automatically
echo.
echo Granting camera, location, and storage permissions...
"%ADB_EXE%" shell pm grant com.transbuddy.app android.permission.CAMERA >nul 2>&1
"%ADB_EXE%" shell pm grant com.transbuddy.app android.permission.ACCESS_FINE_LOCATION >nul 2>&1
"%ADB_EXE%" shell pm grant com.transbuddy.app android.permission.ACCESS_COARSE_LOCATION >nul 2>&1
"%ADB_EXE%" shell pm grant com.transbuddy.app android.permission.READ_MEDIA_IMAGES >nul 2>&1
"%ADB_EXE%" shell pm grant com.transbuddy.app android.permission.READ_EXTERNAL_STORAGE >nul 2>&1

:: 6. Launch TransBuddy Marwadi Splash Activity
echo.
echo Launching TransBuddy on your phone screen...
"%ADB_EXE%" shell am start -n com.transbuddy.app/.controllers.SplashActivity >nul 2>&1

echo.
echo =====================================================================
echo  SUCCESS! TransBuddy is now running on your phone!
echo.
echo  Default Login Credentials:
echo    Username : marwadi
echo    Password : marwadi@121
echo.
echo  Face Recognition:
echo    Tap Navigation Drawer (top-left) -^> "Face Recognition Penalty"
echo =====================================================================
echo.
pause
