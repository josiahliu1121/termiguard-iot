<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  CategoryScale,
  Filler,
  type ChartData,
  type ChartOptions
} from 'chart.js'
import { Line } from 'vue-chartjs'
import type { TelemetryRecord } from '@/types'
import { Activity, Droplets, Battery, Moon, Clock } from '@lucide/vue'

ChartJS.register(Title, Tooltip, Legend, LineElement, LinearScale, PointElement, CategoryScale, Filler)

const props = defineProps<{
  records: TelemetryRecord[]
  stationCode?: string
  isAcousticGated?: boolean
}>()

const activeTab = ref<'moisture_battery' | 'acoustic'>('moisture_battery')

// Slice last 24 events
const slicedRecords = computed(() => {
  return props.records.slice(-24)
})

const hasAcousticData = computed(() => {
  return slicedRecords.value.some(r => (r.acousticScore !== null && r.acousticScore !== undefined) || (r.activityScore !== null && r.activityScore !== undefined))
})

const chartData = computed<ChartData<'line'>>(() => {
  const labels = slicedRecords.value.map(r => {
    const parts = r.timestamp.split(' ')
    const evtType = r.eventType || r.transmissionType
    if (evtType === 'daily_heartbeat') {
      return parts[0].substring(5) // MM-DD
    }
    return parts[1] ? parts[1].substring(0, 5) : r.timestamp
  })

  if (activeTab.value === 'acoustic' && hasAcousticData.value) {
    return {
      labels,
      datasets: [
        {
          label: 'Acoustic Activity (15-min sampling)',
          data: slicedRecords.value.map(r => r.acousticScore ?? r.activityScore ?? 0),
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.15)',
          fill: true,
          tension: 0.35,
          borderWidth: 2.5,
          pointRadius: 4,
          pointBackgroundColor: '#f59e0b'
        },
        {
          label: 'Medium Alert Threshold (50)',
          data: labels.map(() => 50),
          borderColor: 'rgba(245, 158, 11, 0.4)',
          borderDash: [5, 5],
          borderWidth: 1.5,
          pointRadius: 0,
          fill: false
        },
        {
          label: 'High Alert Threshold (75)',
          data: labels.map(() => 75),
          borderColor: 'rgba(239, 68, 68, 0.6)',
          borderDash: [4, 4],
          borderWidth: 1.5,
          pointRadius: 0,
          fill: false
        }
      ]
    }
  } else {
    return {
      labels,
      datasets: [
        {
          label: 'Soil Moisture (%)',
          data: slicedRecords.value.map(r => r.soilMoisture),
          borderColor: '#06b6d4',
          backgroundColor: 'rgba(6, 182, 212, 0.12)',
          fill: true,
          tension: 0.3,
          borderWidth: 2.5,
          pointRadius: 4,
          pointBackgroundColor: '#06b6d4'
        },
        {
          label: 'Moisture Trigger Threshold (65%)',
          data: labels.map(() => 65),
          borderColor: 'rgba(245, 158, 11, 0.5)',
          borderDash: [4, 4],
          borderWidth: 1.5,
          pointRadius: 0,
          fill: false
        },
        {
          label: 'Battery Level (%)',
          data: slicedRecords.value.map(r => r.batteryLevel),
          borderColor: '#10b981',
          borderWidth: 2,
          pointRadius: 3,
          fill: false,
          tension: 0.2
        }
      ]
    }
  }
})

const chartOptions: ChartOptions<'line'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
      labels: {
        color: '#94a3b8',
        boxWidth: 12,
        font: { size: 11 }
      }
    },
    tooltip: {
      backgroundColor: '#0f172a',
      titleColor: '#f8fafc',
      bodyColor: '#cbd5e1',
      borderColor: '#334155',
      borderWidth: 1,
      padding: 10
    }
  },
  scales: {
    x: {
      grid: { color: '#1e293b' },
      ticks: { color: '#64748b', font: { size: 10 } }
    },
    y: {
      min: 0,
      max: 100,
      grid: { color: '#1e293b' },
      ticks: { color: '#64748b', font: { size: 10 } }
    }
  }
}
</script>

<template>
  <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-4 shadow-xl flex flex-col">
    <!-- Header -->
    <div class="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-slate-800">
      <div>
        <h3 class="text-sm font-bold text-white flex items-center gap-2">
          <Activity :size="16" class="text-emerald-400" />
          <span>Underground Telemetry Cadence</span>
          <span v-if="stationCode" class="text-xs px-2 py-0.5 rounded bg-slate-800 text-emerald-300 font-mono">
            {{ stationCode }}
          </span>
        </h3>
        <p class="text-xs text-slate-400">
          {{ isAcousticGated ? '24-hour daily keepalive pings (Moisture + Battery)' : '15-minute triggered acoustic sampling' }}
        </p>
      </div>

      <div class="flex items-center gap-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
        <button
          @click="activeTab = 'moisture_battery'"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition"
          :class="activeTab === 'moisture_battery' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30' : 'text-slate-400 hover:text-white'"
        >
          <Droplets :size="13" />
          <span>Moisture & Battery (Daily)</span>
        </button>

        <button
          @click="activeTab = 'acoustic'"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition"
          :class="activeTab === 'acoustic' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' : 'text-slate-400 hover:text-white'"
        >
          <Activity :size="13" />
          <span>Acoustic Energy (15m)</span>
        </button>
      </div>
    </div>

    <!-- Acoustic Asleep Notice if tab selected on dormant station -->
    <div v-if="activeTab === 'acoustic' && !hasAcousticData" class="flex-1 flex flex-col items-center justify-center p-8 text-center bg-slate-950/60 rounded-xl border border-slate-800/80 my-2">
      <div class="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center text-indigo-400 mb-2">
        <Moon :size="20" />
      </div>
      <h4 class="text-sm font-bold text-white mb-1">No Acoustic Telemetry Transmitted</h4>
      <p class="text-xs text-slate-400 max-w-md">
        Because soil moisture has remained below the 65% trigger threshold, the acoustic piezo circuit has remained in deep sleep to conserve battery. The node only reports once per day.
      </p>
    </div>

    <!-- Chart Canvas -->
    <div v-else class="relative w-full h-64">
      <Line :data="chartData" :options="chartOptions" />
    </div>
  </div>
</template>
