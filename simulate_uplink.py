#!/usr/bin/env python3
"""
TermaGuard.AI - 4G IoT Telemetry Edge Node Simulator
Simulates field ESP32 nodes transmitting 4G telemetry uplink packets to the Spring Boot backend
(POST /api/v1/telemetry/uplink). Backend automatically calls the FastAPI ML Microservice (:5000)
for real-time bioacoustic classification, persists events in MySQL, evaluates temporal persistence,
and broadcasts live updates over WebSocket to the Vue 3 dashboard.
"""

import sys
import time
import json
import argparse
import urllib.request
import urllib.error

# Server configuration
DEFAULT_BACKEND_URL = "http://localhost:8080/api/v1/telemetry/uplink"
API_KEY = "termiguard-node-secret-2026"

# ==============================================================================
# Telemetry Packet Payloads
# ==============================================================================

PACKETS = {
    # 1. Master Station (ST-00) - Sends only Daily Ping (Keepalive Heartbeat)
    "master_ping": {
        "description": "Master Station (ST-00) - 24-Hour Keepalive Ping (Acoustic circuit gated OFF)",
        "payload": {
            "stationId": "ST-00",
            "eventType": "daily_heartbeat",
            "batteryVoltage": 4.18,
            "batteryLevel": 99,
            "soilMoisture": 52.0,
            "acousticScore": None,
            "temperature": 28.5,
            "humidity": 68.0,
            "rssi": -65,
            "snr": 11.5,
            "isMoistureTriggered": False,
            "mfccFeatures": None
        }
    },

    # 2. Station 2 (ST-02) - Termite Detect Signal (High Infestation Threat)
    "station2_detect": {
        "description": "Station 2 (ST-02) - Termite Chewing Detected (Moisture 76.5%, High Acoustic, Positive MFCC)",
        "payload": {
            "stationId": "ST-02",
            "eventType": "15min_triggered_event",
            "batteryVoltage": 3.88,
            "batteryLevel": 84,
            "soilMoisture": 76.5,
            "acousticScore": 86,
            "temperature": 29.8,
            "humidity": 86.5,
            "rssi": -82,
            "snr": 6.8,
            "isMoistureTriggered": True,
            "mfccFeatures": [
                -60.63, 51.08, -16.68, 1.69, -0.68, 30.53,
                9.89, 7.34, -10.79, 4.86, 4.41, 12.87, -1.42
            ]
        }
    },

    # 3. Station 3 (ST-03) - Suspected Signal (Non-termite wood borer / beetle)
    "station3_suspected": {
        "description": "Station 3 (ST-03) - Suspected Biological Signal (Moisture 68.0%, Medium Acoustic, Suspected MFCC)",
        "payload": {
            "stationId": "ST-03",
            "eventType": "15min_triggered_event",
            "batteryVoltage": 3.95,
            "batteryLevel": 89,
            "soilMoisture": 68.0,
            "acousticScore": 52,
            "temperature": 28.7,
            "humidity": 79.2,
            "rssi": -86,
            "snr": 7.1,
            "isMoistureTriggered": True,
            "mfccFeatures": [
                -185.1, 84.42, -29.38, -2.96, -10.73, -5.06,
                -13.59, -6.82, -13.87, -6.88, -13.95, -6.71, -12.26
            ]
        }
    },

    # 4. Station 4 (ST-04) - Safe Signal (Environmental rain/wind noise)
    "station4_safe": {
        "description": "Station 4 (ST-04) - Safe Baseline Signal (Moisture 58.0%, Low Acoustic, Negative MFCC)",
        "payload": {
            "stationId": "ST-04",
            "eventType": "15min_triggered_event",
            "batteryVoltage": 4.05,
            "batteryLevel": 93,
            "soilMoisture": 58.0,
            "acousticScore": 22,
            "temperature": 27.5,
            "humidity": 69.0,
            "rssi": -78,
            "snr": 9.2,
            "isMoistureTriggered": False,
            "mfccFeatures": [
                -137.26, 44.72, -31.56, 23.75, -14.23, 13.59,
                -13.71, 6.0, -6.61, 4.78, -2.23, 4.49, -0.88
            ]
        }
    }
}

def send_packet(url: str, packet_key: str) -> bool:
    packet_info = PACKETS.get(packet_key)
    if not packet_info:
        print(f"[ERROR] Unknown packet key: {packet_key}")
        return False

    desc = packet_info["description"]
    payload = packet_info["payload"]
    st_id = payload["stationId"]
    ev_type = payload["eventType"]

    print(f"\n" + "=" * 70)
    print(f">> TRANSMITTING UPLINK: {desc}")
    print(f"   Station: {st_id} | Type: {ev_type} | Moisture: {payload['soilMoisture']}% | Acoustic: {payload['acousticScore']}")
    print(f"   Endpoint: {url}")
    print("=" * 70)

    json_bytes = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=json_bytes,
        headers={
            "Content-Type": "application/json",
            "X-Node-API-Key": API_KEY,
            "User-Agent": "ESP32-Simulator/1.0"
        },
        method="POST"
    )

    start_time = time.time()
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            latency = (time.time() - start_time) * 1000
            resp_bytes = response.read()
            resp_json = json.loads(resp_bytes.decode("utf-8"))

            print(f"[OK] HTTP {response.status} ({latency:.1f} ms)")
            print(f"     Status:     {resp_json.get('status')}")
            print(f"     Event ID:   {resp_json.get('eventId')}")
            print(f"     ServerTime: {resp_json.get('serverTime')}")
            
            cfg = resp_json.get("config", {})
            print(f"     Sync Config: MoistureThreshold={cfg.get('moistureThreshold')}%, "
                  f"SamplingFreq={cfg.get('samplingFrequencyMinutes')}m, "
                  f"HeartbeatInterval={cfg.get('heartbeatIntervalHours')}h")
            return True

    except urllib.error.HTTPError as e:
        latency = (time.time() - start_time) * 1000
        error_body = e.read().decode("utf-8", errors="replace")
        print(f"[FAIL] HTTP {e.code} ({latency:.1f} ms): {error_body}")
        return False
    except urllib.error.URLError as e:
        print(f"[ERROR] Connection failed to {url}: {e.reason}")
        print("        Ensure Spring Boot backend is running on http://localhost:8080")
        return False
    except Exception as e:
        print(f"[ERROR] Unexpected exception: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(description="TermaGuard 4G Edge Station Telemetry Simulator")
    parser.add_argument("--url", default=DEFAULT_BACKEND_URL, help=f"Backend URL (default: {DEFAULT_BACKEND_URL})")
    parser.add_argument("--target", choices=["all", "master", "detect", "suspected", "safe"], default="all",
                        help="Target scenario to transmit (default: all)")
    parser.add_argument("--loop", action="store_true", help="Run simulation continuously in a loop")
    parser.add_argument("--interval", type=int, default=10, help="Loop interval in seconds (default: 10)")
    args = parser.parse_args()

    mapping = {
        "master": ["master_ping"],
        "detect": ["station2_detect"],
        "suspected": ["station3_suspected"],
        "safe": ["station4_safe"],
        "all": ["master_ping", "station2_detect", "station3_suspected", "station4_safe"]
    }

    selected_packets = mapping.get(args.target, mapping["all"])

    print("======================================================================")
    print("       TermaGuard.AI - 4G Cellular Telemetry Edge Simulator           ")
    print("======================================================================")
    print(f"Target URL:    {args.url}")
    print(f"Scenarios:     {', '.join(selected_packets)}")
    print(f"Mode:          {'Continuous Loop (Every ' + str(args.interval) + 's)' if args.loop else 'Single Execution Run'}")

    cycle = 1
    while True:
        if args.loop:
            print(f"\n--- [CYCLE #{cycle}] ---")
        
        for p_key in selected_packets:
            send_packet(args.url, p_key)
            time.sleep(1.0)  # Brief pause between packet bursts

        if not args.loop:
            break

        cycle += 1
        print(f"\nWaiting {args.interval} seconds before next transmission cycle (Press Ctrl+C to stop)...")
        try:
            time.sleep(args.interval)
        except KeyboardInterrupt:
            print("\nSimulation halted by user.")
            break

    print("\n[COMPLETE] Simulation run finished.")

if __name__ == "__main__":
    main()
