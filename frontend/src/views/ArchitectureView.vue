<script setup lang="ts">
import {
  Layers,
  Cpu,
  Radio,
  Server,
  Database,
  ShieldCheck,
  CheckCircle2,
  DollarSign,
  Sun
} from '@lucide/vue'

const services = [
  { name: 'Reverse Proxy / SSL', tech: 'NGINX Alpine', port: '80, 443', role: 'SSL/TLS termination, rate limiting, request routing' },
  { name: 'Core Enterprise Backend', tech: 'Spring Boot 3 (Java 17)', port: '8080 (Internal)', role: 'Business logic, MQTT payload demuxing, REST/WebSocket API' },
  { name: 'AI Inference Microservice', tech: 'FastAPI / PyTorch (Python 3.10)', port: '8000 (Internal)', role: '13-band MFCC tensor forward pass, CNN-LSTM classifier' },
  { name: 'MQTT Broker', tech: 'Mosquitto 2.0', port: '1883, 8883', role: 'QoS 1 telemetry pub/sub ingestion from 5G gateway' },
  { name: 'Primary Time-Series DB', tech: 'TimescaleDB (PostgreSQL 15)', port: '5432 (Internal)', role: 'Station Information table & Event Table hypertables' }
]

const hardwareBom = [
  { item: 'ESP32-S3 Microcontroller Unit', role: 'Edge signal processing & sleep management', qty: '4 units' },
  { item: 'Semtech SX1262 LoRa Transceiver Module', role: 'Point-to-point node-to-master relay hop', qty: '4 units' },
  { item: 'Piezoelectric Ceramic Vibration Sensor', role: 'Sub-surface termite acoustic detection', qty: '4 units' },
  { item: 'Sensirion SHT31-D I2C Sensor', role: 'Microclimate temperature & humidity tracking', qty: '4 units' },
  { item: 'Capacitive Soil Moisture Probe (v1.2)', role: 'Continuous substrate moisture polling', qty: '4 units' },
  { item: 'Monocrystalline Solar Panel + LiPo Charger', role: 'Master Station autonomous solar power', qty: '1 system' },
  { item: 'SX1302 8-Channel LoRaWAN 5G Gateway', role: 'Central site concentrator & cellular backhaul', qty: '1 unit' }
]
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="bg-slate-900/80 p-4 rounded-2xl border border-slate-800">
      <h2 class="text-lg font-black text-white flex items-center gap-2">
        <Layers :size="20" class="text-purple-400" />
        <span>End-to-End System Architecture & Hardware Reference</span>
      </h2>
      <p class="text-xs text-slate-400">Aligned with Project Report Section 3.6 (Cloud & Edge Infrastructure)</p>
    </div>

    <!-- Visual Architecture Flowchart -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-6 shadow-xl">
      <h3 class="text-sm font-bold text-white mb-4 flex items-center gap-2">
        <Server :size="16" class="text-emerald-400" />
        <span>Data Flow & Topology Pipeline</span>
      </h3>

      <div class="grid grid-cols-1 md:grid-cols-5 gap-3 text-xs">
        <!-- Stage 1 -->
        <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 flex flex-col justify-between">
          <div>
            <div class="text-[10px] font-mono text-emerald-400 uppercase font-bold mb-1">1. Sensing Edge</div>
            <h4 class="font-bold text-white mb-2">3 Peripheral Nodes</h4>
            <ul class="text-[11px] text-slate-400 space-y-1 list-disc list-inside">
              <li>Capacitive moisture probe</li>
              <li>Piezo vibration sensor</li>
              <li>SHT31 temp/humidity</li>
              <li>ESP32-S3 deep sleep</li>
            </ul>
          </div>
          <div class="mt-3 pt-2 border-t border-slate-800 text-[10px] text-cyan-400 font-mono">
            LoRa P2P Link &rarr;
          </div>
        </div>

        <!-- Stage 2 -->
        <div class="bg-slate-950 p-4 rounded-xl border border-amber-500/30 flex flex-col justify-between">
          <div>
            <div class="text-[10px] font-mono text-amber-400 uppercase font-bold mb-1">2. Relay Hub</div>
            <h4 class="font-bold text-white mb-2 flex items-center gap-1">
              <span>Master Hub</span>
              <Sun :size="12" class="text-amber-400" />
            </h4>
            <ul class="text-[11px] text-slate-400 space-y-1 list-disc list-inside">
              <li>Relay aggregation</li>
              <li>45-byte binary packet</li>
              <li>Solar powered</li>
              <li>ESP32-S3 controller</li>
            </ul>
          </div>
          <div class="mt-3 pt-2 border-t border-slate-800 text-[10px] text-emerald-400 font-mono">
            LoRaWAN Uplink &rarr;
          </div>
        </div>

        <!-- Stage 3 -->
        <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 flex flex-col justify-between">
          <div>
            <div class="text-[10px] font-mono text-cyan-400 uppercase font-bold mb-1">3. Backhaul</div>
            <h4 class="font-bold text-white mb-2">5G Gateway</h4>
            <ul class="text-[11px] text-slate-400 space-y-1 list-disc list-inside">
              <li>SX1302 Concentrator</li>
              <li>MQTT Broker Publish</li>
              <li>4G/LTE + 5G NR</li>
              <li>QoS Level 1 Delivery</li>
            </ul>
          </div>
          <div class="mt-3 pt-2 border-t border-slate-800 text-[10px] text-purple-400 font-mono">
            Cellular MQTT &rarr;
          </div>
        </div>

        <!-- Stage 4 -->
        <div class="bg-slate-950 p-4 rounded-xl border border-purple-500/30 flex flex-col justify-between">
          <div>
            <div class="text-[10px] font-mono text-purple-400 uppercase font-bold mb-1">4. Cloud Core</div>
            <h4 class="font-bold text-white mb-2">Docker VPS</h4>
            <ul class="text-[11px] text-slate-400 space-y-1 list-disc list-inside">
              <li>Spring Boot (8080)</li>
              <li>FastAPI / PyTorch (8000)</li>
              <li>TimescaleDB (5432)</li>
              <li>Mosquitto MQTT</li>
            </ul>
          </div>
          <div class="mt-3 pt-2 border-t border-slate-800 text-[10px] text-emerald-400 font-mono">
            REST / WebSocket &rarr;
          </div>
        </div>

        <!-- Stage 5 -->
        <div class="bg-slate-950 p-4 rounded-xl border border-emerald-500/40 flex flex-col justify-between">
          <div>
            <div class="text-[10px] font-mono text-emerald-400 uppercase font-bold mb-1">5. Presentation</div>
            <h4 class="font-bold text-white mb-2">Vue 3 Dashboard</h4>
            <ul class="text-[11px] text-slate-400 space-y-1 list-disc list-inside">
              <li>Satellite GIS map</li>
              <li>Real-time telemetry</li>
              <li>Verify &rarr; Treat SOP</li>
              <li>MFCC spectral visualizer</li>
            </ul>
          </div>
          <div class="mt-3 pt-2 border-t border-slate-800 text-[10px] text-emerald-400 font-semibold">
            Operator Interface
          </div>
        </div>
      </div>
    </div>

    <!-- Two Database Tables Schema Breakdown -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-6 shadow-xl space-y-4">
      <div class="flex items-center justify-between border-b border-slate-800 pb-3">
        <div>
          <h3 class="text-sm font-bold text-white flex items-center gap-2">
            <Database :size="16" class="text-cyan-400" />
            <span>Relational Database Schema (TimescaleDB / PostgreSQL 15)</span>
          </h3>
          <p class="text-xs text-slate-400">Strict separation of mutable station entity metadata vs append-only time-series event telemetry</p>
        </div>
        <span class="text-xs font-mono font-bold px-2.5 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-800">
          WebSocket &rarr; Monitors Event Table
        </span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
        <!-- Table 1: Station Information Table -->
        <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2">
          <div class="flex items-center justify-between border-b border-slate-800 pb-2">
            <span class="font-bold text-white text-sm font-sans flex items-center gap-1.5">
              <span class="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
              Table 1: stations (Station Info)
            </span>
            <span class="text-[10px] text-emerald-400 font-sans font-semibold">Modifiable via PUT API</span>
          </div>
          <p class="text-[11px] font-sans text-slate-400">
            Stores station configuration and spatial coordinates. Modifiable by operators directly from the Plantation GIS Map.
          </p>
          <div class="bg-slate-900/90 p-2.5 rounded-lg border border-slate-800 text-[11px] text-slate-300 space-y-1">
            <div><strong class="text-amber-400">station_id</strong>: VARCHAR (PK)</div>
            <div><strong>code</strong>: VARCHAR (e.g. ST-00, ST-01)</div>
            <div><strong>name / description</strong>: TEXT (Descriptive notes)</div>
            <div><strong>coordinates</strong>: FLOAT[2] [latitude, longitude]</div>
            <div><strong>station_type</strong>: VARCHAR ('master' | 'peripheral')</div>
            <div><strong>plantation_zone</strong>: VARCHAR (e.g. Zone A - Core)</div>
            <div><strong>tree_tag</strong>: VARCHAR (e.g. PW-RMB-0101)</div>
            <div><strong>moisture_threshold</strong>: NUMERIC (65.0%)</div>
          </div>
          <div class="text-[10px] text-slate-500 font-sans">
            REST Endpoints: <span class="text-cyan-300">GET /api/v1/stations</span> &bull; <span class="text-emerald-300">PUT /api/v1/stations/{id}</span>
          </div>
        </div>

        <!-- Table 2: Event Table -->
        <div class="bg-slate-950 p-4 rounded-xl border border-amber-500/30 space-y-2">
          <div class="flex items-center justify-between border-b border-slate-800 pb-2">
            <span class="font-bold text-white text-sm font-sans flex items-center gap-1.5">
              <span class="w-2.5 h-2.5 rounded-full bg-amber-400 animate-ping"></span>
              Table 2: events (Time-Series Hypertable)
            </span>
            <span class="text-[10px] text-amber-400 font-sans font-semibold">Streamed via WebSocket</span>
          </div>
          <p class="text-[11px] font-sans text-slate-400">
            Append-only telemetry log. The WebSocket server monitors this table and broadcasts each row insert to the frontend.
          </p>
          <div class="bg-slate-900/90 p-2.5 rounded-lg border border-slate-800 text-[11px] text-slate-300 space-y-1">
            <div><strong class="text-amber-400">event_id</strong>: UUID (PK)</div>
            <div><strong>station_id</strong>: VARCHAR (FK &rarr; stations.station_id)</div>
            <div><strong>timestamp</strong>: TIMESTAMPTZ (chunking key)</div>
            <div><strong>event_type</strong>: VARCHAR ('daily_heartbeat' | '15min_triggered')</div>
            <div><strong>soil_moisture</strong>: NUMERIC (Capacitive probe %)</div>
            <div><strong>acoustic_score</strong>: NUMERIC (Nullable: null when gated asleep)</div>
            <div><strong>battery_level & voltage</strong>: NUMERIC, NUMERIC</div>
            <div><strong>predict_level</strong>: VARCHAR (Safe, Low, Medium, High)</div>
          </div>
          <div class="text-[10px] text-slate-500 font-sans">
            Stream: <span class="text-amber-400">WS /ws/telemetry (event_inserted)</span> &bull; <span class="text-cyan-300">GET /api/v1/stations/{id}/events</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Table 3.6: Cloud Microservice Stack -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-5 shadow-xl">
      <h3 class="text-sm font-bold text-white mb-3">Table 3.6: Cloud Service Stack & Container Allocation</h3>
      
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs text-slate-300">
          <thead class="bg-slate-950 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800 font-mono">
            <tr>
              <th class="py-2.5 px-3">Service Role</th>
              <th class="py-2.5 px-3">Base Environment</th>
              <th class="py-2.5 px-3">Exposed Port</th>
              <th class="py-2.5 px-3">Role & Functionality</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-800">
            <tr v-for="srv in services" :key="srv.name" class="hover:bg-slate-800/30">
              <td class="py-3 px-3 font-semibold text-white">{{ srv.name }}</td>
              <td class="py-3 px-3 font-mono text-cyan-300">{{ srv.tech }}</td>
              <td class="py-3 px-3 font-mono text-amber-300">{{ srv.port }}</td>
              <td class="py-3 px-3 text-slate-400">{{ srv.role }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Hardware Bill of Materials & Budget Compliance -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-5 shadow-xl">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-bold text-white">Pilot Hardware Bill of Materials (BOM)</h3>
        <span class="text-xs px-2.5 py-0.5 rounded-full font-bold bg-emerald-950 text-emerald-300 border border-emerald-800">
          Budget Compliance: ~RM10,480 Industry Allocation
        </span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div
          v-for="hw in hardwareBom"
          :key="hw.item"
          class="bg-slate-950 p-3 rounded-xl border border-slate-800 flex items-center justify-between text-xs"
        >
          <div>
            <div class="font-bold text-white">{{ hw.item }}</div>
            <div class="text-[11px] text-slate-400">{{ hw.role }}</div>
          </div>
          <span class="font-mono text-cyan-400 font-semibold px-2 py-1 bg-slate-900 rounded border border-slate-800">
            {{ hw.qty }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
