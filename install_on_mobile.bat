@echo off
echo ===================================================
echo   TransBuddy App - One-Click Mobile Installer
echo ===================================================
echo.
set ADB_PATH=C:\Users\ASUS\AppData\Local\Android\Sdk\platform-tools\adb.exe

echo 1. Checking connected mobile device...
"%ADB_PATH%" devices
echo.

echo 2. Installing TransBuddy APK to your phone...
"%ADB_PATH%" install -r "app\build\outputs\apk\debug\app-debug.apk"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Installation failed or phone not authorized yet.
    echo Please unlock your phone and tap "Allow USB Debugging" when prompted, then run this again.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo 3. Launching TransBuddy Login Screen on your phone...
"%ADB_PATH%" shell am start -n com.transbuddy.app/.controllers.LoginActivity

echo.
echo ===================================================
echo   SUCCESS! TransBuddy is now running on your phone!
echo   Login with:
echo     Username: marwadi
echo     Password: marwadi@121
echo ===================================================
pause
