@echo off
echo Starting Node.js task sequence...

echo Running scripts/accountLogInRequestBad1.js
node scripts/accountLogInRequestBad1.js
echo scripts/accountLogInRequestBad1.js finished.
echo.

timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountLogInRequest1.js
node scripts/accountLogInRequest1.js
echo scripts/accountLogInRequest1.js finished.
echo.


timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountLogInRequest2.js with arguments
node scripts/accountLogInRequest2.js arg1 arg2
echo scripts/accountLogInRequest2.js finished.
echo.

timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountLogInRequest3.js
node scripts/accountLogInRequest3.js
echo scripts/accountLogInRequest3.js finished.
echo.


timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountLogInRequest4.js
node scripts/accountLogInRequest4.js
echo scripts/accountLogInRequest4.js finished.
echo.

echo All tasks completed!
pause