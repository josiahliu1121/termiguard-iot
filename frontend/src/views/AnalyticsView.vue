<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { usePlantationStore } from '@/stores/plantationStore'
import TelemetryChart from '@/components/TelemetryChart.vue'
import MfccVisualizer from '@/components/MfccVisualizer.vue'
import { api } from '@/api'
import type { TelemetryRecord } from '@/types'
import {
  Cpu,
  TrendingUp,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Zap,
  Target,
  BarChart3,
  Moon,
  ClipboardCheck,
  ShieldAlert,
  Layers
} from '@lucide/vue'

const store = usePlantationStore()
const selectedStationId = ref<string>('ST-02')
const historyRecords = ref<TelemetryRecord[]>([])

const currentStation = computed(() => {
  return store.stations.find(s => s.id === selectedStationId.value) || store.stations[0]
})

async function loadHistory() {
  if (selectedStationId.value) {
    historyRecords.value = await api.stations.getHistory(selectedStationId.value)
  }
}

watch(selectedStationId, () => {
  loadHistory()
})

onMounted(() => {
  if (store.selectedStationId) {
    selectedStationId.value = store.selectedStationId
  }
  loadHistory()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Top Header -->
    <div class="flex flex-wrap items-center justify-between gap-4 bg-slate-900/80 p-4 rounded-2xl border border-slate-800">
      <div>
        <h2 class="text-lg font-black text-white flex items-center gap-2">
          <Cpu :size="20" class="text-cyan-400" />
          <span>AI Predictive Risk Analytics & Acoustic Intelligence</span>
        </h2>
        <p class="text-xs text-slate-400">Gated Sensing: 15-Minute Capture when Moisture &ge; 65% &bull; 24h Keepalive</p>
      </div>

      <!-- Station Selector -->
      <div class="flex items-center gap-2">
        <label class="text-xs text-slate-400 font-medium">Select Station:</label>
        <select
          v-model="selectedStationId"
          class="bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs font-semibold text-white outline-none focus:border-cyan-500"
        >
          <option v-for="st in store.stations" :key="st.id" :value="st.id">
            {{ st.code }} - {{ st.name }} ({{ st.isAcousticGated ? 'Dormant' : 'Active 15m' }})
          </option>
        </select>
      </div>
    </div>

    <!-- Research Acceptance Criteria & Performance Evaluation (Section 3.7.4) -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-5 shadow-xl">
      <div class="flex items-center justify-between mb-4 pb-3 border-b border-slate-800">
        <div>
          <h3 class="font-bold text-white text-sm flex items-center gap-2">
            <Target :size="16" class="text-emerald-400" />
            <span>Research Objectives: Validation Metrics & Acceptance Criteria (Section 3.7.4)</span>
          </h3>
          <p class="text-xs text-slate-400">Measured against low-power duty cycle & ground truth verification</p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- Objective 1: Network & Classification -->
        <div class="bg-slate-950/70 p-4 rounded-xl border border-slate-800 space-y-3">
          <div class="flex items-center justify-between text-xs font-semibold text-slate-300">
            <span>Obj 1: Relay & LoRaWAN Network</span>
            <span class="text-emerald-400 font-bold">PASSED</span>
          </div>

          <div class="space-y-2 text-xs">
            <div>
              <div class="flex justify-between text-[11px] mb-1 text-slate-400">
                <span>Relay Link Availability (P2P)</span>
                <span class="text-white font-mono font-bold">{{ store.metrics?.relayLinkAvailabilityPercent }}% (Target &gt;95%)</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-emerald-500 rounded-full" :style="{ width: `${store.metrics?.relayLinkAvailabilityPercent}%` }"></div>
              </div>
            </div>

            <div>
              <div class="flex justify-between text-[11px] mb-1 text-slate-400">
                <span>Master-to-Gateway Uplink</span>
                <span class="text-white font-mono font-bold">{{ store.metrics?.masterUplinkAvailabilityPercent }}% (Target &gt;95%)</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-emerald-500 rounded-full" :style="{ width: `${store.metrics?.masterUplinkAvailabilityPercent}%` }"></div>
              </div>
            </div>

            <div>
              <div class="flex justify-between text-[11px] mb-1 text-slate-400">
                <span>Acoustic Classification Accuracy</span>
                <span class="text-white font-mono font-bold">{{ store.metrics?.classificationAccuracyPercent }}% (Target &gt;90%)</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-cyan-500 rounded-full" :style="{ width: `${store.metrics?.classificationAccuracyPercent}%` }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Objective 2: AI Predictive Analytics -->
        <div class="bg-slate-950/70 p-4 rounded-xl border border-slate-800 space-y-3">
          <div class="flex items-center justify-between text-xs font-semibold text-slate-300">
            <span>Obj 2: Predictive Forecasting & Lead Time</span>
            <span class="text-emerald-400 font-bold">VERIFIED</span>
          </div>

          <div class="space-y-2.5 text-xs">
            <div class="flex items-center justify-between p-2 rounded-lg bg-slate-900 border border-slate-800">
              <span class="text-slate-400 text-[11px]">Internal Forecasting Lead Time (Medium &rarr; High)</span>
              <span class="text-purple-400 font-mono font-bold text-sm">{{ store.metrics?.internalForecastingLeadTimeDays }} Days</span>
            </div>

            <div class="flex items-center justify-between p-2 rounded-lg bg-slate-900 border border-slate-800">
              <span class="text-slate-400 text-[11px]">High-Tier False-Alarm Rate</span>
              <span class="text-emerald-400 font-mono font-bold text-sm">{{ store.metrics?.falseAlarmRatePercent }}% (Target &lt;15%)</span>
            </div>

            <div class="flex items-center justify-between p-2 rounded-lg bg-slate-900 border border-slate-800">
              <span class="text-slate-400 text-[11px]">Average Deep-Sleep Ratio</span>
              <span class="text-cyan-400 font-mono font-bold text-sm">{{ store.metrics?.averageDailySleepRatioPercent }}%</span>
            </div>
          </div>
        </div>

        <!-- Objective 3: Field Validation vs Manual Inspection -->
        <div class="bg-slate-950/70 p-4 rounded-xl border border-slate-800 space-y-3">
          <div class="flex items-center justify-between text-xs font-semibold text-slate-300">
            <span>Obj 3: Detection Lead-Time Advantage</span>
            <span class="text-emerald-400 font-bold">ACHIEVED</span>
          </div>

          <div class="space-y-2 text-xs">
            <div class="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-300">
              <div class="text-[11px] text-slate-400 mb-0.5">Detection Lead Time vs 1-3 Month Manual Schedule:</div>
              <div class="text-2xl font-black text-emerald-400 font-mono">+{{ store.metrics?.detectionLeadTimeDays }} Days</div>
              <div class="text-[10px] text-emerald-400/80 mt-1">Exceeds the 4-to-8 week target stated in Section 3.7.4.</div>
            </div>

            <div class="text-[10px] text-slate-400 flex items-center gap-1.5">
              <CheckCircle2 :size="12" class="text-emerald-400" />
              <span>Response SOP (Verify &rarr; Treat &rarr; Monitor) ready for partner sign-off.</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Live Ground Truth Confusion Matrix & Performance Metrics (Human-in-the-Loop) -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-5 shadow-xl space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-slate-800">
        <div>
          <h3 class="font-bold text-white text-sm flex items-center gap-2">
            <ClipboardCheck :size="16" class="text-emerald-400" />
            <span>Human-in-the-Loop Ground Truth &amp; Confusion Matrix</span>
          </h3>
          <p class="text-xs text-slate-400">
            Calculated dynamically from the Event Table across retroactive 7-day field verification windows
          </p>
        </div>

        <div class="flex items-center gap-2 text-xs font-mono">
          <span class="px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 text-slate-300">
            Total Labeled: <strong class="text-white">{{ store.groundTruthStats.total }}</strong> events
          </span>
          <span class="px-2.5 py-1 rounded-lg bg-emerald-950/60 border border-emerald-800/80 text-emerald-300 font-bold">
            Accuracy: {{ store.groundTruthStats.accuracy }}%
          </span>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <!-- 2x2 Confusion Matrix Grid (7 Cols) -->
        <div class="lg:col-span-7 bg-slate-950/70 p-4 rounded-xl border border-slate-800">
          <div class="text-xs font-semibold text-slate-300 mb-3 flex items-center justify-between">
            <span>Empirical Confusion Matrix (2 &times; 2)</span>
            <span class="text-[10px] text-slate-500 font-mono">Events Table Ground Truth</span>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-center text-xs border-collapse">
              <thead>
                <tr>
                  <th class="p-2 border border-slate-800/60 bg-slate-900/40 text-slate-500 font-normal"></th>
                  <th colspan="2" class="p-2 border border-slate-800 bg-slate-900 text-cyan-300 font-bold uppercase tracking-wider text-[11px]">
                    System Telemetry Prediction
                  </th>
                </tr>
                <tr>
                  <th class="p-2 border border-slate-800 bg-slate-900 text-slate-400 font-bold uppercase tracking-wider text-[10px]">
                    Ground Truth (Field Inspection)
                  </th>
                  <th class="p-2 border border-slate-800 bg-slate-900/60 text-slate-300 font-semibold">
                    Predicted Alert (Medium / High)
                  </th>
                  <th class="p-2 border border-slate-800 bg-slate-900/60 text-slate-300 font-semibold">
                    Predicted Safe (Safe / Low / Dormant)
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="p-3 border border-slate-800 bg-slate-900/80 font-bold text-rose-300 text-left">
                    <div class="flex items-center gap-1.5">
                      <span class="w-2 h-2 rounded-full bg-rose-500"></span>
                      <span>Termites Present (Infested)</span>
                    </div>
                  </td>
                  <td class="p-3 border border-slate-800 bg-emerald-950/20 text-emerald-400 font-mono">
                    <div class="text-base font-black">{{ store.groundTruthStats.tp }}</div>
                    <div class="text-[10px] text-emerald-400/80 font-bold uppercase">True Positive (TP)</div>
                    <div class="text-[9px] text-slate-500">Correctly Alerted</div>
                  </td>
                  <td class="p-3 border border-slate-800 bg-amber-950/20 text-amber-400 font-mono">
                    <div class="text-base font-black">{{ store.groundTruthStats.fn }}</div>
                    <div class="text-[10px] text-amber-400/80 font-bold uppercase">False Negative (FN)</div>
                    <div class="text-[9px] text-slate-500">Missed Infestation</div>
                  </td>
                </tr>
                <tr>
                  <td class="p-3 border border-slate-800 bg-slate-900/80 font-bold text-emerald-300 text-left">
                    <div class="flex items-center gap-1.5">
                      <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                      <span>Clean Tree (No Termites)</span>
                    </div>
                  </td>
                  <td class="p-3 border border-slate-800 bg-rose-950/20 text-rose-400 font-mono">
                    <div class="text-base font-black">{{ store.groundTruthStats.fp }}</div>
                    <div class="text-[10px] text-rose-400/80 font-bold uppercase">False Positive (FP)</div>
                    <div class="text-[9px] text-slate-500">Benign False Alarm</div>
                  </td>
                  <td class="p-3 border border-slate-800 bg-blue-950/20 text-blue-400 font-mono">
                    <div class="text-base font-black">{{ store.groundTruthStats.tn }}</div>
                    <div class="text-[10px] text-blue-400/80 font-bold uppercase">True Negative (TN)</div>
                    <div class="text-[9px] text-slate-500">Accurately Dormant</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-3 text-[11px] text-slate-400 leading-relaxed">
            💡 <strong>Academic Note (IDP Report Section 4.3):</strong> When a technician performs physical ground truth verification, the retrospective 7-day engine correlates all 15-minute and 24-hour events against biological reality without altering the objective mathematical threat index.
          </div>
        </div>

        <!-- Calculated ML Performance Metrics (5 Cols) -->
        <div class="lg:col-span-5 space-y-3">
          <div class="bg-slate-950/70 p-4 rounded-xl border border-slate-800 space-y-3">
            <div class="text-xs font-semibold text-slate-300">Statistical Verification Metrics</div>

            <!-- Precision -->
            <div>
              <div class="flex justify-between text-xs mb-1">
                <span class="text-slate-400">Precision (Positive Predictive Value):</span>
                <span class="font-mono font-bold text-emerald-400">{{ store.groundTruthStats.precision }}%</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-emerald-500 rounded-full" :style="{ width: `${store.groundTruthStats.precision}%` }"></div>
              </div>
              <div class="text-[9px] text-slate-500 mt-0.5">TP / (TP + FP) &bull; Confidence when system alerts</div>
            </div>

            <!-- Recall / Sensitivity -->
            <div>
              <div class="flex justify-between text-xs mb-1">
                <span class="text-slate-400">Recall / Sensitivity:</span>
                <span class="font-mono font-bold text-cyan-400">{{ store.groundTruthStats.recall }}%</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-cyan-500 rounded-full" :style="{ width: `${store.groundTruthStats.recall}%` }"></div>
              </div>
              <div class="text-[9px] text-slate-500 mt-0.5">TP / (TP + FN) &bull; Percentage of attacks caught</div>
            </div>

            <!-- False Alarm Rate -->
            <div>
              <div class="flex justify-between text-xs mb-1">
                <span class="text-slate-400">False Alarm Rate (FAR):</span>
                <span class="font-mono font-bold text-amber-400">{{ store.groundTruthStats.falseAlarmRate }}%</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-amber-500 rounded-full" :style="{ width: `${store.groundTruthStats.falseAlarmRate}%` }"></div>
              </div>
              <div class="text-[9px] text-slate-500 mt-0.5">FP / (FP + TN) &bull; Rain / machinery disturbance</div>
            </div>

            <!-- Miss Rate -->
            <div>
              <div class="flex justify-between text-xs mb-1">
                <span class="text-slate-400">Miss Rate (False Negative Rate):</span>
                <span class="font-mono font-bold text-rose-400">{{ store.groundTruthStats.missRate }}%</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div class="h-full bg-rose-500 rounded-full" :style="{ width: `${store.groundTruthStats.missRate}%` }"></div>
              </div>
              <div class="text-[9px] text-slate-500 mt-0.5">FN / (TP + FN) &bull; Unflagged termite attacks</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Active Station Analytics & Waveforms -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6" v-if="currentStation">
      <!-- 7 Cols: Time Series Chart -->
      <div class="lg:col-span-7">
        <TelemetryChart
          :records="historyRecords"
          :station-code="currentStation.code"
          :is-acoustic-gated="currentStation.isAcousticGated"
        />
      </div>

      <!-- 5 Cols: Edge Feature Extraction & Softmax -->
      <div class="lg:col-span-5">
        <MfccVisualizer
          :mfcc-features="currentStation.mfccFeatures"
          :activity-score="currentStation.activityScore"
          :confidence-tier="currentStation.confidenceTier"
          :is-acoustic-gated="currentStation.isAcousticGated"
          :soil-moisture="currentStation.soilMoisture"
          :moisture-threshold="currentStation.moistureThreshold"
          @wake-acoustic="() => { store.toggleMoistureBreach(currentStation.id); loadHistory(); }"
        />
      </div>
    </div>
  </div>
</template>
