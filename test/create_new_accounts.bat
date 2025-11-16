@echo off
echo Starting Node.js task sequence...

echo Running scripts/accountSignInRequest1.js
node scripts/accountSignInRequest1.js
echo scripts/accountSignInRequest1.js finished.
echo.


timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountSignInRequest2.js with arguments
node scripts/accountSignInRequest2.js arg1 arg2
echo scripts/accountSignInRequest2.js finished.
echo.

timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountSignInRequest3.js
node scripts/accountSignInRequest3.js
echo scripts/accountSignInRequest3.js finished.
echo.


timeout /t 3 /nobreak >nul
echo.

echo Running scripts/accountSignInRequest4.js
node scripts/accountSignInRequest4.js
echo scripts/accountSignInRequest4.js finished.
echo.

echo All tasks completed!
pause