<script setup lang="ts">
import { computed } from 'vue'
import { Cpu, Zap, Moon, Droplets, ShieldCheck, Play } from '@lucide/vue'

const props = defineProps<{
  mfccFeatures?: number[]
  activityScore: number | null
  confidenceTier: string
  isAcousticGated?: boolean
  soilMoisture?: number
  moistureThreshold?: number
}>()

const coefficients = computed(() => {
  if (props.mfccFeatures && props.mfccFeatures.length === 13) {
    return props.mfccFeatures
  }
  return [-12.5, 8.4, 2.1, -1.5, 0.8, -0.4, 0.2, 0.1, -0.2, 0.1, -0.1, 0.0, 0.1]
})

const probabilities = computed(() => {
  const act = props.activityScore || 0
  if (act > 75) {
    return {
      termite: 92.4,
      rain: 4.2,
      noise: 3.4,
      label: 'Confirmed Subterranean Termite Foraging Signature'
    }
  } else if (act > 45) {
    return {
      termite: 74.8,
      rain: 14.2,
      noise: 11.0,
      label: 'Early Subterranean Acoustic Anomaly Detected (15m Cadence)'
    }
  } else {
    return {
      termite: 6.5,
      rain: 22.1,
      noise: 71.4,
      label: 'Safe Baseline / Environmental Micro-Settling'
    }
  }
})
</script>

<template>
  <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-4 shadow-xl flex flex-col justify-between">
    <!-- Header -->
    <div class="flex items-center justify-between gap-3 mb-3 pb-3 border-b border-slate-800">
      <div>
        <h4 class="text-sm font-bold text-white flex items-center gap-2">
          <Cpu :size="16" class="text-cyan-400" />
          <span>Acoustic MFCC & CNN-LSTM Classifier</span>
        </h4>
        <p class="text-[11px] text-slate-400">15-minute sampling frequency (Gated by soil moisture threshold)</p>
      </div>

      <span
        class="text-xs px-2.5 py-0.5 rounded-full font-mono font-bold border"
        :class="isAcousticGated ? 'bg-slate-800 text-slate-400 border-slate-700' : 'bg-amber-950 text-amber-300 border-amber-800'"
      >
        {{ isAcousticGated ? 'Circuit Asleep' : 'Active (15m Cadence)' }}
      </span>
    </div>

    <!-- STATE 1: GATED IN DEEP SLEEP -->
    <div v-if="isAcousticGated" class="bg-slate-950/80 p-5 rounded-xl border border-slate-800 text-center space-y-3 my-2">
      <div class="w-12 h-12 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center mx-auto">
        <Moon :size="24" />
      </div>

      <div>
        <h5 class="text-sm font-bold text-white">Piezo Acoustic Sensor in Deep Sleep</h5>
        <p class="text-xs text-slate-400 max-w-sm mx-auto mt-1 leading-relaxed">
          Substrate moisture is currently <strong class="text-cyan-400">{{ soilMoisture || 56 }}%</strong>, which is below the <strong class="text-amber-400">{{ moistureThreshold || 65 }}%</strong> trigger threshold.
        </p>
      </div>

      <div class="p-3 bg-slate-900 rounded-lg border border-slate-800/80 text-[11px] text-slate-300 text-left space-y-1 font-mono">
        <div class="flex justify-between">
          <span class="text-slate-400">Acoustic Telemetry:</span>
          <span class="text-slate-200">Suppressed (Zero transmissions)</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Current Reporting Cadence:</span>
          <span class="text-emerald-400">Once a day (24h Keepalive)</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Next 15m Sampling:</span>
          <span class="text-slate-400">Waiting for moisture trigger &ge; {{ moistureThreshold || 65 }}%</span>
        </div>
      </div>
    </div>

    <!-- STATE 2: ACTIVE 15-MINUTE SAMPLING -->
    <div v-else>
      <!-- 13-band MFCC Spectral Fingerprint Visualizer -->
      <div class="mb-4">
        <div class="flex items-center justify-between text-[11px] text-slate-400 mb-1.5 font-mono">
          <span>MFCC Band 0 (Energy)</span>
          <span class="text-amber-400 font-semibold">15m Window Sample</span>
          <span>Band 12 (Harmonics)</span>
        </div>
        
        <div class="grid grid-cols-13 gap-1 h-20 items-end bg-slate-950/70 p-2 rounded-xl border border-slate-800/80">
          <div
            v-for="(val, idx) in coefficients"
            :key="idx"
            class="flex flex-col items-center justify-end h-full group relative"
          >
            <div
              class="w-full rounded-t transition-all duration-300"
              :style="{
                height: `${Math.min(100, Math.max(15, (val + 25) * 2))}%`,
                backgroundColor: val > 6 ? '#ef4444' : val > 0 ? '#06b6d4' : '#64748b'
              }"
            ></div>
            
            <div class="absolute -top-7 hidden group-hover:flex bg-slate-800 text-[9px] px-1.5 py-0.5 rounded text-white font-mono whitespace-nowrap z-10">
              c[{{ idx }}]: {{ val.toFixed(1) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Softmax Probabilities -->
      <div class="space-y-2 bg-slate-950/60 p-3 rounded-xl border border-slate-800/80">
        <div class="flex items-center justify-between text-xs mb-1">
          <span class="text-slate-300 font-semibold flex items-center gap-1.5">
            <Zap :size="13" class="text-amber-400" />
            FastAPI Softmax Output:
          </span>
          <span class="text-[11px] font-medium" :class="probabilities.termite > 50 ? 'text-rose-400 font-bold' : 'text-emerald-400'">
            {{ probabilities.label }}
          </span>
        </div>

        <div>
          <div class="flex justify-between text-[11px] mb-0.5">
            <span class="text-slate-400">Coptotermes Subterranean Acoustic Signal</span>
            <span class="font-bold text-rose-400 font-mono">{{ probabilities.termite }}%</span>
          </div>
          <div class="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
            <div
              class="h-full bg-gradient-to-r from-amber-500 to-rose-500 rounded-full transition-all duration-500"
              :style="{ width: `${probabilities.termite}%` }"
            ></div>
          </div>
        </div>

        <div>
          <div class="flex justify-between text-[11px] mb-0.5">
            <span class="text-slate-400">Rainfall / Moisture Noise</span>
            <span class="font-semibold text-cyan-400 font-mono">{{ probabilities.rain }}%</span>
          </div>
          <div class="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
            <div
              class="h-full bg-cyan-500 rounded-full transition-all duration-500"
              :style="{ width: `${probabilities.rain}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid-cols-13 {
  grid-template-columns: repeat(13, minmax(0, 1fr));
}
</style>
