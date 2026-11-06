<#
====================================================================
  One-Click: Full Rebuild + Launch + Detailed Monitoring
  Usage: Double-click oneclick.bat (or right-click .ps1 -> Run)

  Pipeline:
   [1/8] Kill old AI, free port 8082
   [2/8] ADB connect + forward tcp:8999
   [3/8] Uninstall old APK
   [4/8] Flutter clean + pub get + build apk
   [5/8] Install APK + launch app
   [6/8] Start Python AI Agent on :8082
   [7/8] Launch 4 monitoring terminals
   [8/8] Done banner

  Monitors (step 7):
    [A] AI REACT TRACE  -> Parsed tool-call timeline (UTF8 safe)
    [B] AI RAW LOGS     -> Full loguru tail (UTF8 safe)
    [C] FLUTTER API     -> adb logcat filtered (UTF8 safe)
    [D] DB SNAPSHOT     -> Poll with change detection
====================================================================
#>

# Force UTF8 output everywhere - fixes all Chinese garbled text
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# ======================== ENVIRONMENT ========================
$env:JAVA_HOME                = "D:\yyrj\Java\jdk-17"
$env:ANDROID_HOME             = "D:\yyrj\AndroidSDK"
$env:ANDROID_SDK_ROOT         = "D:\yyrj\AndroidSDK"
$env:PUB_CACHE                = "D:\pub-cache"
$env:GRADLE_USER_HOME         = "D:\gradle-home"
$env:FLUTTER_STORAGE_BASE_URL = "https://mirrors.cloud.tencent.com/flutter"

$ADB        = "D:\yyrj\MuMuPlayer\nx_main\adb.exe"
$FLUTTER    = "D:\yyrj\flutter\bin\flutter.bat"
$PYTHON     = "python"
$AI_DIR     = "d:\BaiduNetdiskDownload\class\ProjectAssignment\app\backend\ai-service"
$FRONT_DIR  = "d:\BaiduNetdiskDownload\class\ProjectAssignment\app\frontend"
$APP_PKG    = "com.example.experiment_app.frontend"
$LOG_DIR    = "$AI_DIR\logs"

# ======================== HELPERS ========================
function Step($n, $total, $msg) {
    Write-Host ""
    Write-Host ("===== [{0}/{1}] {2} =====" -f $n, $total, $msg) -ForegroundColor Cyan
}
function Done($msg)  { Write-Host "  [OK]   $msg" -ForegroundColor Green }
function Warn($msg)  { Write-Host "  [WARN] $msg" -ForegroundColor Yellow }
function Fail($msg)  { Write-Host "  [FAIL] $msg" -ForegroundColor Red }

# ======================== 1. KILL OLD AI ========================
Step 1 8 "Kill old AI + free port 8082"
Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
Start-Sleep -Seconds 2
Done "Port 8082 freed"

# ======================== 2. ADB + FORWARD ========================
Step 2 8 "ADB connect + forward tcp:8999"
& $ADB connect 127.0.0.1:7555 2>$null | Out-Null
Start-Sleep -Seconds 1
& $ADB -s 127.0.0.1:7555 forward tcp:8999 tcp:8999 2>&1 | Out-Null
Done "Forward tcp:8999 -> tcp:8999"

# ======================== 3. UNINSTALL ========================
Step 3 8 "Uninstall old APK"
& $ADB -s 127.0.0.1:7555 uninstall "$APP_PKG" 2>&1 | Out-Null
Done "Uninstalled"

# ======================== 4. FLUTTER BUILD ========================
Step 4 8 "Flutter clean + pub get + build apk"
Set-Location $FRONT_DIR
& $FLUTTER clean 2>&1 | Select-Object -Last 1 | Out-Null
& $FLUTTER pub get 2>&1 | Select-Object -Last 1 | Out-Null
Write-Host "  flutter build apk --debug ..." -ForegroundColor Gray
& $FLUTTER build apk --debug 2>&1 | Select-Object -Last 3

$APK = Get-ChildItem -Path "$FRONT_DIR\build" -Recurse -Filter "app-debug.apk" | Select-Object -First 1
if (-not $APK) { Fail "APK not found! Abort"; exit 1 }
Done "Build OK: $($APK.FullName.Split('\')[-1])"

# ======================== 5. INSTALL + LAUNCH ========================
Step 5 8 "Install APK + launch app"
& $ADB -s 127.0.0.1:7555 install -r $APK.FullName 2>&1 | Out-Null
Done "Installed"
& $ADB -s 127.0.0.1:7555 shell "am start -n $APP_PKG/.MainActivity" 2>&1 | Out-Null
Done "App launched"
Write-Host "  Waiting for LocalApiServer (15s)..." -ForegroundColor Gray
Start-Sleep -Seconds 15
try { Invoke-RestMethod -Uri "http://localhost:8999/api/health" -TimeoutSec 3 | Out-Null; Done "LocalApiServer ready" }
catch { Warn "LocalApiServer may not be up yet" }

# ======================== 6. START PYTHON AI ========================
Step 6 8 "Start Python AI Agent on :8082"
Set-Location $AI_DIR
Start-Process -FilePath $PYTHON -ArgumentList "-m","uvicorn","main:app","--host","0.0.0.0","--port","8082" `
    -WindowStyle Minimized -WorkingDirectory $AI_DIR
Write-Host "  Health check (10s)..." -ForegroundColor Gray

$aiReady = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        $h = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/ai/health" -TimeoutSec 2
        if ($h.status -eq "healthy") { $aiReady = $true; break }
    } catch {}
}
if ($aiReady) { Done "AI Agent HEALTHY" } else { Warn "AI may not be ready" }

# ======================== 7. LAUNCH 4 MONITORS ========================
Step 7 8 "Launch 4 detailed monitoring terminals"

$monDir = Join-Path $env:TEMP "ai-monitor"
New-Item -ItemType Directory -Force -Path $monDir | Out-Null
$logFile = "$LOG_DIR\ai-agent_$(Get-Date -Format 'yyyy-MM-dd').log"

# ============================================================
# Monitor A: AI REACT TRACE (robust - no .NET StreamReader!)
# Uses: Get-Content -Tail N -Wait with UTF8 encoding
# ============================================================
$tracePs1 = Join-Path $monDir "monitor-react-trace.ps1"
@"
# Force UTF8 - prevents Chinese garbled text
[Console]::OutputEncoding = [Text.Encoding]::UTF8

Set-Location '$AI_DIR'
Write-Host ""
Write-Host "  AI REACT TRACE  (parsed tool-call timeline)" -ForegroundColor Magenta
Write-Host "  Ctrl+C to stop" -ForegroundColor Gray
Write-Host "========================================================" -ForegroundColor Magenta
Write-Host ""

`$f = '$logFile'

# Wait for log file to appear
while (-not (Test-Path `$f)) {
    Write-Host "Waiting for log file `$f ..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}
Write-Host "Log file found. Parsing new lines only..." -ForegroundColor Green
Write-Host ""

# Skip existing content (tail 0 lines), then wait for new
# We use -Tail 0 to skip all existing content, -Wait for new
Get-Content -Path `$f -Tail 0 -Wait -Encoding UTF8 | ForEach-Object {
    `$line = `$_
    `$ts = Get-Date -Format 'HH:mm:ss'

    if (`$line -match 'msg=(.*)') {
        Write-Host ("[{0}] USER MSG     {1}" -f `$ts, `$Matches[1]) -ForegroundColor Cyan
    }
    elseif (`$line -match '本轮调用 (\d+) 个工具: \[(.*?)\]') {
        `$tools = `$Matches[2] -replace "'", ""
        Write-Host ("[{0}] TOOL CALL     {1}" -f `$ts, `$tools) -ForegroundColor Yellow
    }
    elseif (`$line -match '\[Backend GET\] (.*?) params=(.*)') {
        Write-Host ("[{0}]   GET        {1}  params={2}" -f `$ts, `$Matches[1], `$Matches[2]) -ForegroundColor DarkGray
    }
    elseif (`$line -match '\[Backend POST\] (.*?) body=(.*)') {
        Write-Host ("[{0}]   POST       {1}  body={2}" -f `$ts, `$Matches[1], `$Matches[2]) -ForegroundColor Gray
    }
    elseif (`$line -match '\[Backend DELETE\] (.*)') {
        Write-Host ("[{0}]   DEL        {1}" -f `$ts, `$Matches[1]) -ForegroundColor DarkGray
    }
    elseif (`$line -match '工具 (\S+) 执行成功') {
        Write-Host ("[{0}] TOOL OK       {1}" -f `$ts, `$Matches[1]) -ForegroundColor Green
    }
    elseif (`$line -match '工具 (\S+) 执行失败') {
        Write-Host ("[{0}] TOOL FAIL     {1}" -f `$ts, `$Matches[1]) -ForegroundColor Red
    }
    elseif (`$line -match 'ChatService.*完成: tools_called=(\d+), reply_len=(\d+)') {
        Write-Host ("[{0}] CHAT DONE     tools={1} reply_len={2}" -f `$ts, `$Matches[1], `$Matches[2]) -ForegroundColor Green
        Write-Host "--------------------------------------------------------" -ForegroundColor Magenta
    }
    elseif (`$line -match 'ERROR|Traceback|Exception') {
        Write-Host `$line -ForegroundColor Red
    }
}
"@ | Set-Content -Path $tracePs1 -Encoding UTF8
Start-Process powershell -ArgumentList "-NoExit","-ExecutionPolicy","Bypass","-File",$tracePs1
Done "Monitor A: AI ReAct Trace"

# ============================================================
# Monitor B: AI RAW LOGS (full tail, UTF8 safe)
# ============================================================
$rawPs1 = Join-Path $monDir "monitor-raw-logs.ps1"
@"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
Set-Location '$AI_DIR'
Write-Host ""
Write-Host "  AI RAW LOGS  (full loguru tail - encoding: UTF8)" -ForegroundColor Cyan
Write-Host "  Ctrl+C to stop" -ForegroundColor Gray
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

`$f = '$logFile'
if (Test-Path `$f) {
    Get-Content -Path `$f -Tail 80 -Wait -Encoding UTF8
} else {
    Write-Host "Waiting for log file..." -ForegroundColor Yellow
    while (-not (Test-Path `$f)) { Start-Sleep -Seconds 2 }
    Get-Content -Path `$f -Tail 50 -Wait -Encoding UTF8
}
"@ | Set-Content -Path $rawPs1 -Encoding UTF8
Start-Process powershell -ArgumentList "-NoExit","-ExecutionPolicy","Bypass","-File",$rawPs1
Done "Monitor B: AI Raw Logs"

# ============================================================
# Monitor C: FLUTTER API WATCH (adb logcat, UTF8 safe)
# ============================================================
$flutterPs1 = Join-Path $monDir "monitor-flutter-api.ps1"
@"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
Write-Host ""
Write-Host "  FLUTTER API WATCH  (adb logcat)" -ForegroundColor Cyan
Write-Host "  Filters: LocalApiServer, Database, API calls" -ForegroundColor Gray
Write-Host "  Ctrl+C to stop" -ForegroundColor Gray
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

& '$ADB' -s 127.0.0.1:7555 logcat -c 2>`$null
Write-Host "Log buffer cleared. Watching..." -ForegroundColor Green
Write-Host ""

& '$ADB' -s 127.0.0.1:7555 logcat -v time | Select-String -Pattern 'LocalApiServer|DatabaseService|GET /api|POST /api|PUT /api|DELETE /api|api/health|api/bills|api/categories|api/budgets|api/statistics|sqlite|Insert|Update|Delete|请求|写入|数据库|种子'
"@ | Set-Content -Path $flutterPs1 -Encoding UTF8
Start-Process powershell -ArgumentList "-NoExit","-ExecutionPolicy","Bypass","-File",$flutterPs1
Done "Monitor C: Flutter API + DB"

# ============================================================
# Monitor D: DB SNAPSHOT + CHANGE DETECTION
# ============================================================
$dbPs1 = Join-Path $monDir "monitor-db.ps1"
@"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
Write-Host ""
Write-Host "  DB SNAPSHOT  (poll + change detection)" -ForegroundColor Green
Write-Host "  Ctrl+C to stop" -ForegroundColor Gray
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""

`$FL = 'http://localhost:8999'
`$AI = 'http://localhost:8082'
`$prevBill = -1
`$prevCat  = -1
`$prevBud  = -1

while (`$true) {
    try {
        `$ts = Get-Date -Format 'HH:mm:ss'
        `$b = Invoke-RestMethod -Uri "`$FL/api/bills" -TimeoutSec 2
        `$c = Invoke-RestMethod -Uri "`$FL/api/categories" -TimeoutSec 2
        `$now = Get-Date
        `$bud = Invoke-RestMethod -Uri "`$FL/api/budgets?year=`$(`$now.Year)&month=`$(`$now.Month)" -TimeoutSec 2
        `$h = Invoke-RestMethod -Uri "`$AI/api/v1/ai/health" -TimeoutSec 2

        `$bc = if (`$b.data) { `$b.data.Count } else { 0 }
        `$cc = if (`$c.data) { `$c.data.Count } else { 0 }
        `$buc = if (`$bud.data) { `$bud.data.Count } else { 0 }

        `$color = [ConsoleColor]::Green
        `$note = ""
        if (`$prevBill -ge 0 -and `$bc -ne `$prevBill) {
            `$diff = `$bc - `$prevBill
            `$color = if (`$diff -gt 0) { [ConsoleColor]::Yellow } else { [ConsoleColor]::Red }
            `$note = " BILLS (`$diff)"
        }
        if (`$prevCat -ge 0 -and `$cc -ne `$prevCat) { `$note += " CATS" }
        if (`$prevBud -ge 0 -and `$buc -ne `$prevBud) { `$note += " BUDS" }

        Write-Host ("[{0}] bills={1,3} cats={2,2} buds={3,2} AI={4} {5}" -f `$ts, `$bc, `$cc, `$buc, `$h.status, `$note) -ForegroundColor `$color

        # Show new bill details
        if (`$prevBill -ge 0 -and `$bc -gt `$prevBill) {
            `$latest = `$b.data | Sort-Object id -Descending | Select-Object -First 1
            Write-Host ("      NEW: #{0} [{1}] {2} {3} remark={4}" -f `$latest.id, `$latest.transactionDate, `$latest.categoryName, `$latest.amount, `$latest.remark) -ForegroundColor Cyan
        }

        `$prevBill = `$bc; `$prevCat = `$cc; `$prevBud = `$buc
    } catch {
        Write-Host ("[{0}] waiting..." -f (Get-Date -Format 'HH:mm:ss')) -ForegroundColor DarkGray
    }
    Start-Sleep -Seconds 5
}
"@ | Set-Content -Path $dbPs1 -Encoding UTF8
Start-Process powershell -ArgumentList "-NoExit","-ExecutionPolicy","Bypass","-File",$dbPs1
Done "Monitor D: DB Snapshot + Change Detection"

Write-Host ""
Write-Host "  Monitor scripts: $monDir" -ForegroundColor Gray

# ======================== 8. DONE ========================
Step 8 8 "All systems go"

Write-Host ""
Write-Host "============================================================" -ForegroundColor White
Write-Host "  One-Click Launch Complete!" -ForegroundColor White
Write-Host ""
Write-Host "  App:       Look for Smart Ledger icon in emulator" -ForegroundColor White
Write-Host "  Login:     test@demo.com / 123456" -ForegroundColor White
Write-Host "  AI:        Open AI Assistant tab in emulator" -ForegroundColor White
Write-Host ""
Write-Host "  4 Monitoring Terminals Launched:" -ForegroundColor White
Write-Host "    [A] AI ReAct Trace    -> Parsed tool timeline" -ForegroundColor Magenta
Write-Host "    [B] AI Raw Logs       -> Full loguru tail" -ForegroundColor Cyan
Write-Host "    [C] Flutter API Watch -> adb logcat" -ForegroundColor Cyan
Write-Host "    [D] DB Snapshot       -> Poll + change detection" -ForegroundColor Green
Write-Host ""
Write-Host "  Stop AI:" -ForegroundColor Gray
Write-Host "    Get-NetTCPConnection -LocalPort 8082 | ForEach-Object { Stop-Process -Id `$_.OwningProcess -Force }" -ForegroundColor Gray
Write-Host "  Re-apply forward (after emulator reboot):" -ForegroundColor Gray
Write-Host "    adb -s 127.0.0.1:7555 forward tcp:8999 tcp:8999" -ForegroundColor Gray
Write-Host "============================================================" -ForegroundColor White

Set-Location $FRONT_DIR
