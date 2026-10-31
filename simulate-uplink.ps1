# ==============================================================================
# TermaGuard.AI - 4G Edge Telemetry Node Simulator (PowerShell)
# Transmits realistic cellular uplink telemetry packets from:
#   1. Master Station (ST-00) - Daily Ping (heartbeat only)
#   2. Station 2 (ST-02)       - Termite Detected (15m triggered, high acoustic & termite MFCC)
#   3. Station 3 (ST-03)       - Suspected Signal (15m triggered, medium acoustic & borer MFCC)
#   4. Station 4 (ST-04)       - Safe Signal (15m triggered, low acoustic & rain/wind MFCC)
# ==============================================================================

param (
    [string]$Url = "http://localhost:8080/api/v1/telemetry/uplink",
    [string]$Target = "all", # all, master, detect, suspected, safe
    [switch]$Loop,
    [int]$Interval = 10
)

$ApiKey = "termiguard-node-secret-2026"
$Headers = @{
    "Content-Type"   = "application/json"
    "X-Node-API-Key" = $ApiKey
    "User-Agent"     = "ESP32-PowerShell-Simulator/1.0"
}

$Packets = @{
    "master_ping" = @{
        Name = "Master Station (ST-00) - Daily Ping (Heartbeat Only)"
        Body = @{
            stationId = "ST-00"
            eventType = "daily_heartbeat"
            batteryVoltage = 4.18
            batteryLevel = 99
            soilMoisture = 52.0
            acousticScore = $null
            temperature = 28.5
            humidity = 68.0
            rssi = -65
            snr = 11.5
            isMoistureTriggered = $false
            mfccFeatures = $null
        }
    }
    "station2_detect" = @{
        Name = "Station 2 (ST-02) - Termite Detected (High Risk / Positive MFCC)"
        Body = @{
            stationId = "ST-02"
            eventType = "15min_triggered_event"
            batteryVoltage = 3.88
            batteryLevel = 84
            soilMoisture = 76.5
            acousticScore = 86
            temperature = 29.8
            humidity = 86.5
            rssi = -82
            snr = 6.8
            isMoistureTriggered = $true
            mfccFeatures = @(-60.63, 51.08, -16.68, 1.69, -0.68, 30.53, 9.89, 7.34, -10.79, 4.86, 4.41, 12.87, -1.42)
        }
    }
    "station3_suspected" = @{
        Name = "Station 3 (ST-03) - Suspected Signal (Medium Risk / Borer MFCC)"
        Body = @{
            stationId = "ST-03"
            eventType = "15min_triggered_event"
            batteryVoltage = 3.95
            batteryLevel = 89
            soilMoisture = 68.0
            acousticScore = 52
            temperature = 28.7
            humidity = 79.2
            rssi = -86
            snr = 7.1
            isMoistureTriggered = $true
            mfccFeatures = @(-185.1, 84.42, -29.38, -2.96, -10.73, -5.06, -13.59, -6.82, -13.87, -6.88, -13.95, -6.71, -12.26)
        }
    }
    "station4_safe" = @{
        Name = "Station 4 (ST-04) - Safe Baseline Signal (Low Risk / Ambient Noise MFCC)"
        Body = @{
            stationId = "ST-04"
            eventType = "15min_triggered_event"
            batteryVoltage = 4.05
            batteryLevel = 93
            soilMoisture = 58.0
            acousticScore = 22
            temperature = 27.5
            humidity = 69.0
            rssi = -78
            snr = 9.2
            isMoistureTriggered = $false
            mfccFeatures = @(-137.26, 44.72, -31.56, 23.75, -14.23, 13.59, -13.71, 6.0, -6.61, 4.78, -2.23, 4.49, -0.88)
        }
    }
}

function Send-TelemetryPacket($key) {
    $pkt = $Packets[$key]
    if (-not $pkt) { return }

    Write-Host "`n>> Transmitting: $($pkt.Name)" -ForegroundColor Cyan
    $json = $pkt.Body | ConvertTo-Json -Depth 5 -Compress

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $resp = Invoke-RestMethod -Uri $Url -Method POST -Headers $Headers -Body $json -TimeoutSec 5
        $stopwatch.Stop()

        Write-Host "   [OK] HTTP 200 ($([Math]::Round($stopwatch.Elapsed.TotalMilliseconds, 1)) ms)" -ForegroundColor Green
        Write-Host "        Status:     $($resp.status)" -ForegroundColor Gray
        Write-Host "        Event ID:   $($resp.eventId)" -ForegroundColor Gray
        Write-Host "        Station ID: $($resp.stationId)" -ForegroundColor Yellow
        Write-Host "        ServerTime: $($resp.serverTime)" -ForegroundColor Gray
        Write-Host "        SyncConfig: Threshold=$($resp.config.moistureThreshold)% | Sampling=$($resp.config.samplingFrequencyMinutes)m | Heartbeat=$($resp.config.heartbeatIntervalHours)h" -ForegroundColor DarkCyan
    } catch {
        $stopwatch.Stop()
        Write-Host "   [FAIL] Request error: $_" -ForegroundColor Red
        Write-Host "          Ensure Spring Boot backend is active on $Url" -ForegroundColor DarkYellow
    }
}

# Determine sequence
$keys = switch ($Target.ToLower()) {
    "master"    { @("master_ping") }
    "detect"    { @("station2_detect") }
    "suspected" { @("station3_suspected") }
    "safe"      { @("station4_safe") }
    default     { @("master_ping", "station2_detect", "station3_suspected", "station4_safe") }
}

Write-Host "========================================================================" -ForegroundColor Green
Write-Host "          TermaGuard.AI 4G Cellular Telemetry Field Simulator          " -ForegroundColor White
Write-Host "========================================================================" -ForegroundColor Green
Write-Host "Target Endpoint: $Url" -ForegroundColor Gray
Write-Host "Target Packets:  $($keys -join ', ')" -ForegroundColor Gray
Write-Host "Mode:            $(if ($Loop) { 'Continuous Loop (Interval: ' + $Interval + 's)' } else { 'Single Execution Run' })" -ForegroundColor Gray

$cycle = 1
do {
    if ($Loop) {
        Write-Host "`n--- [TRANSMISSION CYCLE #$cycle] ---" -ForegroundColor Yellow
    }

    foreach ($k in $keys) {
        Send-TelemetryPacket $k
        Start-Sleep -Milliseconds 800
    }

    if ($Loop) {
        $cycle++
        Write-Host "`nWaiting $Interval seconds before next cycle (Press Ctrl+C to stop)..." -ForegroundColor DarkGray
        Start-Sleep -Seconds $Interval
    }
} while ($Loop)

Write-Host "`n========================================================================" -ForegroundColor Green
Write-Host "Transmission completed successfully." -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Green
