<script setup lang="ts">
import { computed } from 'vue'
import type { Station } from '@/types'
import { formatCleanTimeAgo } from '@/mocks/mockData'
import {
  Radio,
  Sun,
  Battery,
  BatteryCharging,
  Droplets,
  Thermometer,
  Activity,
  Moon,
  Zap,
  ArrowRight,
  Clock,
  ShieldAlert,
  ClipboardCheck
} from '@lucide/vue'

const props = defineProps<{
  station: Station
  isSelected: boolean
}>()

const emit = defineEmits<{
  (e: 'select', station: Station): void
  (e: 'edit', station: Station): void
  (e: 'openVerify', station: Station): void
}>()

const tierConfig = computed(() => {
  if (props.station.isAcousticGated) {
    return {
      bg: 'bg-slate-800/80 border-slate-700 text-slate-300',
      dot: 'bg-slate-500',
      badge: 'Safe (Gated Asleep)'
    }
  }

  const level = props.station.threatLevel || props.station.confidenceTier
  switch (level) {
    case 'High':
      return {
        bg: 'bg-rose-500/15 border-rose-500/40 text-rose-300',
        dot: 'bg-rose-500 animate-ping',
        badge: 'High Threat (Confirmed)'
      }
    case 'Medium':
      return {
        bg: 'bg-amber-500/15 border-amber-500/40 text-amber-300',
        dot: 'bg-amber-500',
        badge: 'Medium Risk (Suspected)'
      }
    case 'Low':
      return {
        bg: 'bg-blue-500/15 border-blue-500/40 text-blue-300',
        dot: 'bg-blue-500',
        badge: 'Low Risk (Observing)'
      }
    default:
      return {
        bg: 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300',
        dot: 'bg-emerald-500',
        badge: 'Safe Baseline'
      }
  }
})
</script>

<template>
  <div
    @click="emit('select', station)"
    class="bg-slate-900/90 rounded-2xl border p-4 shadow-xl transition-all duration-200 cursor-pointer flex flex-col justify-between"
    :class="[
      isSelected
        ? 'border-emerald-500 ring-2 ring-emerald-500/30 bg-slate-900 shadow-emerald-950/40'
        : station.isProfileComplete === false
          ? 'border-indigo-500/60 bg-indigo-950/20 hover:border-indigo-400'
          : 'border-slate-800 hover:border-slate-700 hover:bg-slate-850'
    ]"
  >
    <!-- Card Header -->
    <div>
      <div class="flex items-start justify-between gap-2 mb-2">
        <div class="flex items-center gap-2">
          <div
            class="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs"
            :class="station.type === 'master' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' : station.isProfileComplete === false ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40' : 'bg-slate-800 text-slate-200'"
          >
            {{ station.code.replace('ST-0', '').replace('ST-', '') }}
          </div>
          <div>
            <div class="flex items-center gap-1.5">
              <h4 class="font-bold text-white text-sm">{{ station.code }}</h4>
              <span v-if="station.type === 'master'" class="flex items-center gap-1 px-1.5 py-0.2 rounded bg-amber-950 text-amber-400 text-[10px] font-semibold border border-amber-800">
                <Sun :size="10" /> Master Hub
              </span>
              <span v-else-if="station.isProfileComplete === false" class="px-1.5 py-0.2 rounded bg-indigo-950 text-indigo-300 text-[9px] font-bold border border-indigo-700/60 uppercase">
                New Go-Live
              </span>
            </div>
            <p v-if="station.isProfileComplete === false" class="text-[11px] text-indigo-300 font-semibold flex items-center gap-1">
              <Radio :size="11" class="animate-pulse text-indigo-400" />
              <span>Awaiting Profile Setup</span>
            </p>
            <p v-else class="text-[11px] text-slate-400 font-mono">{{ station.treeTag }} &bull; {{ station.zone }}</p>
          </div>
        </div>

        <!-- Operating State Badge & Human Verification Pill -->
        <div class="flex flex-col items-end gap-1">
          <span
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold border"
            :class="tierConfig.bg"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="tierConfig.dot"></span>
            <span>{{ tierConfig.badge }}</span>
          </span>

          <span
            v-if="station.humanVerificationStatus && station.humanVerificationStatus !== 'unverified'"
            class="px-2 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider border"
            :class="[
              (station.humanVerificationStatus === 'verified_infestation' || station.humanVerificationStatus === 'field_verified_infestation') ? 'bg-rose-950/60 text-rose-300 border-rose-500/50' : '',
              station.humanVerificationStatus === 'treatment_start' ? 'bg-amber-950/60 text-amber-300 border-amber-500/50' : '',
              station.humanVerificationStatus === 'monitor' ? 'bg-indigo-950/60 text-indigo-300 border-indigo-500/50' : '',
              station.humanVerificationStatus === 'verified_clean' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-500/50' : '',
              station.humanVerificationStatus === 'false_alarm' ? 'bg-purple-950/60 text-purple-300 border-purple-500/50' : ''
            ]"
          >
            <template v-if="station.humanVerificationStatus === 'verified_clean'">
              ✓ {{ formatCleanTimeAgo(station.verifiedCleanAt) }}
            </template>
            <template v-else-if="station.humanVerificationStatus === 'field_verified_infestation' || station.humanVerificationStatus === 'verified_infestation'">
              ● Infestation Verified
            </template>
            <template v-else-if="station.humanVerificationStatus === 'treatment_start'">
              ● In Treatment
            </template>
            <template v-else-if="station.humanVerificationStatus === 'monitor'">
              ● Monitoring (40d)
            </template>
            <template v-else-if="station.humanVerificationStatus === 'false_alarm'">
              ⚠ False Alarm Cleared
            </template>
          </span>
        </div>
      </div>

      <!-- Quick Metrics Grid -->
      <div class="grid grid-cols-2 gap-2 my-3">
        <!-- Soil Moisture (Continuous Poll Trigger) -->
        <div class="bg-slate-950/70 p-2.5 rounded-xl border" :class="station.soilMoisture >= station.moistureThreshold ? 'border-amber-500/40 bg-amber-950/10' : 'border-slate-800'">
          <div class="flex items-center justify-between text-[11px] text-slate-400 mb-1">
            <span class="flex items-center gap-1"><Droplets :size="12" class="text-cyan-400" /> Soil Moisture</span>
            <span class="font-bold font-mono text-cyan-300">{{ station.soilMoisture }}%</span>
          </div>
          <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="station.soilMoisture >= station.moistureThreshold ? 'bg-amber-500' : 'bg-cyan-500'"
              :style="{ width: `${station.soilMoisture}%` }"
            ></div>
          </div>
          <div class="text-[9px] text-slate-400 mt-1 flex justify-between">
            <span>Threshold: {{ station.moistureThreshold }}%</span>
            <span :class="station.soilMoisture >= station.moistureThreshold ? 'text-amber-400 font-bold' : 'text-slate-500'">
              {{ station.soilMoisture >= station.moistureThreshold ? 'TRIGGERED' : 'Below Trg' }}
            </span>
          </div>
        </div>

        <!-- Acoustic Sensor State (Gated Sleep vs 15m Sampling) -->
        <div class="bg-slate-950/70 p-2.5 rounded-xl border border-slate-800">
          <div class="flex items-center justify-between text-[11px] text-slate-400 mb-1">
            <span class="flex items-center gap-1">
              <component :is="station.isAcousticGated ? Moon : Activity" :size="12" :class="station.isAcousticGated ? 'text-indigo-400' : 'text-amber-400'" />
              <span>Acoustic (15m)</span>
            </span>
            <span v-if="!station.isAcousticGated" class="font-bold font-mono text-amber-400">
              {{ station.activityScore }}/100
            </span>
            <span v-else class="text-[10px] font-mono text-indigo-300 font-semibold">
              Deep Sleep
            </span>
          </div>

          <div v-if="!station.isAcousticGated" class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="station.activityScore! > 65 ? 'bg-rose-500' : 'bg-amber-500'"
              :style="{ width: `${station.activityScore}%` }"
            ></div>
          </div>
          <div v-else class="text-[9px] text-slate-500 italic mt-1">
            Gated OFF &bull; Zero LoRa traffic
          </div>
          
          <div v-if="!station.isAcousticGated" class="text-[9px] text-amber-400 font-medium mt-1">
            Sampling every 15 mins
          </div>
        </div>
      </div>

      <!-- Option A + B: Rolling Infestation Threat Index (ITI) & Temporal Persistence Ribbon -->
      <div class="mb-3 px-3 py-2 rounded-xl bg-slate-950/70 border border-slate-800">
        <div class="flex items-center justify-between text-[11px] mb-1">
          <span class="text-slate-400 font-medium flex items-center gap-1">
            <ShieldAlert :size="12" :class="station.threatIndex >= 80 ? 'text-rose-400' : station.threatIndex >= 55 ? 'text-amber-400' : station.threatIndex >= 30 ? 'text-blue-400' : 'text-emerald-400'" />
            <span>Threat Index (ITI)</span>
          </span>
          <span class="font-mono font-bold" :class="station.threatIndex >= 80 ? 'text-rose-400' : station.threatIndex >= 55 ? 'text-amber-400' : station.threatIndex >= 30 ? 'text-blue-400' : 'text-emerald-400'">
            {{ station.threatIndex ?? 0 }}/100
          </span>
        </div>
        <!-- Rolling score progress bar -->
        <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
          <div
            class="h-full rounded-full transition-all duration-300"
            :class="station.threatIndex >= 80 ? 'bg-rose-500' : station.threatIndex >= 55 ? 'bg-amber-500' : station.threatIndex >= 30 ? 'bg-blue-500' : 'bg-emerald-500'"
            :style="{ width: `${station.threatIndex ?? 0}%` }"
          ></div>
        </div>
        <!-- Persistence Window Summary -->
        <div class="text-[10px] text-slate-400 mt-1.5 flex items-center justify-between gap-1">
          <span class="truncate text-slate-300">{{ station.windowSummary || 'Evaluating rolling cycles...' }}</span>
          <span v-if="station.windowHits !== undefined" class="font-mono text-[9px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 shrink-0">
            {{ station.windowHits }}/{{ station.windowTotal || 6 }} hits
          </span>
        </div>
      </div>
    </div>

    <!-- Card Footer -->
    <div class="pt-2 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
      <div class="flex items-center gap-3">
        <!-- Cadence Info -->
        <span class="flex items-center gap-1 text-[10px] text-slate-400 font-mono">
          <Clock :size="11" class="text-slate-500" />
          {{ station.isAcousticGated ? '24h Daily Ping' : '15m Sampling' }}
        </span>

        <!-- Battery -->
        <span class="flex items-center gap-1 text-[11px]">
          <component :is="station.isSolarCharging ? BatteryCharging : Battery" :size="13" class="text-emerald-400" />
          {{ station.batteryLevel }}%
        </span>
      </div>

      <!-- Actions -->
      <div class="flex items-center gap-2">
        <button
          v-if="station.isProfileComplete === false"
          type="button"
          @click.stop="emit('edit', station)"
          class="px-2 py-0.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-[10px] font-bold flex items-center gap-1 transition shadow shadow-indigo-950/40"
          title="Complete Station Profile Metadata"
        >
          <Radio :size="11" />
          <span>Key In Info</span>
        </button>

        <button
          type="button"
          @click.stop="emit('openVerify', station)"
          class="px-2 py-0.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-[10px] font-semibold flex items-center gap-1 border border-slate-700 transition"
          title="Field Inspection / Ground Truth Audit"
        >
          <ClipboardCheck :size="11" class="text-emerald-400" />
          <span>Audit</span>
        </button>

        <!-- Select Indicator -->
        <span class="flex items-center gap-1 text-emerald-400 font-semibold text-[11px]">
          <span>Inspect</span>
          <ArrowRight :size="12" />
        </span>
      </div>
    </div>
  </div>
</template>
