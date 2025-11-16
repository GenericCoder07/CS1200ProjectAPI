@echo off
echo Starting Node.js task sequence...

echo Running scripts/openAICannedRequest.js
node scripts/openAICannedRequest.js
echo scripts/openAICannedRequest.js finished.
echo.

timeout /t 10 /nobreak >nul
echo.

echo Running scripts/openAIRequest.js
node scripts/openAIRequest.js
echo scripts/openAIRequest.js finished.
echo.

echo All tasks completed!
pause