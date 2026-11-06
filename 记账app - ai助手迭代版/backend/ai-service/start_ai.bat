@echo off
cd /d "d:\BaiduNetdiskDownload\class\ProjectAssignment\app\backend\ai-service"
python -m uvicorn main:app --host 0.0.0.0 --port 8082 >> D:\ai.log 2>&1
