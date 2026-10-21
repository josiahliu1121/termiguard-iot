<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePlantationStore } from '@/stores/plantationStore'
import PlantationMap from '@/components/PlantationMap.vue'
import StationCard from '@/components/StationCard.vue'
import TelemetryChart from '@/components/TelemetryChart.vue'
import MfccVisualizer from '@/components/MfccVisualizer.vue'
import StationModal from '@/components/StationModal.vue'
import FieldVerificationModal from '@/components/FieldVerificationModal.vue'
import { api } from '@/api'
import type { Station, AlertEvent, TelemetryRecord } from '@/types'
import {
  ShieldAlert,
  Activity,
  Droplets,
  Clock,
  Radio,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  TrendingUp,
  Cpu,
  Zap,
  Moon,
  BatteryCharging,
  Heart,
  ClipboardCheck,
  History,
  ChevronDown,
  MapPin,
  Layers
} from '@lucide/vue'

const store = usePlantationStore()
const router = useRouter()
const route = useRoute()

const isStationModalOpen = ref(false)
const stationToEdit = ref<Station | null>(null)
const isVerifyModalOpen = ref(false)
const stationForVerify = ref<string>('')
const stationHistory = ref<TelemetryRecord[]>([])

// Plantation Zone Filter State (Displays one zone at once to prevent clutter)
const selectedZone = ref<string>('')

// Unique list of available plantation zones from stations
const availableZones = computed(() => {
  const zones = new Set<string>()
  store.stations.forEach(s => {
    if (s.plantationZone) {
      zones.add(s.plantationZone)
    }
  })
  return Array.from(zones)
})

// Count stations in a given zone
function getZoneStationCount(zone: string): number {
  return store.stations.filter(s => s.plantationZone === zone).length
}

// Auto-initialize selectedZone to selectedStation's zone or first available zone
watch(
  () => [store.stations, store.selectedStation],
  () => {
    if (!selectedZone.value) {
      if (store.selectedStation?.plantationZone) {
        selectedZone.value = store.selectedStation.plantationZone
      } else if (availableZones.value.length > 0) {
        selectedZone.value = availableZones.value[0]
      }
    }
  },
  { immediate: true, deep: true }
)

// Handle manual zone change from dropdown
function handleZoneChange(newZone: string) {
  selectedZone.value = newZone
  if (newZone !== 'all') {
    const stationsInZone = store.stations.filter(s => s.plantationZone === newZone)
    // If the currently selected station is outside this zone, select the first station of this zone
    if (stationsInZone.length > 0 && !stationsInZone.some(s => s.id === store.selectedStationId)) {
      handleSelectStation(stationsInZone[0])
    }
  }
}

// Filtered stations for cards display
const filteredStations = computed(() => {
  if (!selectedZone.value || selectedZone.value === 'all') {
    return store.stations
  }
  return store.stations.filter(s => s.plantationZone === selectedZone.value)
})

function handleOpenAddStation() {
  stationToEdit.value = null
  isStationModalOpen.value = true
}

function handleConfigureStation(station: Station) {
  stationToEdit.value = station
  isStationModalOpen.value = true
}

function handleOpenVerify(station?: Station) {
  stationForVerify.value = station ? station.id : store.selectedStationId
  isVerifyModalOpen.value = true
}

function handleOpenVerifyForAlert(alert: AlertEvent) {
  stationForVerify.value = alert.stationId
  isVerifyModalOpen.value = true
}

async function loadSelectedStationHistory(stationId?: string) {
  const targetId = stationId || store.selectedStationId
  if (targetId) {
    stationHistory.value = await api.stations.getHistory(targetId)
  }
}

async function handleSelectStation(station: Station) {
  store.selectStation(station.id)
  if (selectedZone.value !== 'all' && station.plantationZone && station.plantationZone !== selectedZone.value) {
    selectedZone.value = station.plantationZone
  }
  await loadSelectedStationHistory(station.id)
}

async function handleSaveStation(data: any) {
  if (stationToEdit.value) {
    await store.updateStationInfo(stationToEdit.value.id, data)
  } else {
    await store.addNewStation(data)
  }
  isStationModalOpen.value = false
  stationToEdit.value = null
}

async function handleSimulateNewDiscovery() {
  const nextNum = store.stations.length
  const nextId = `ST-0${nextNum}`
  await store.simulateNewStationDiscovery(nextId)
}

function handleWakeAcoustic() {
  if (store.selectedStation) {
    store.toggleMoistureBreach(store.selectedStation.id)
    loadSelectedStationHistory(store.selectedStation.id)
  }
}

watch(() => store.selectedStationId, (newId) => {
  loadSelectedStationHistory(newId)
})

onMounted(async () => {
  if (route.query.station) {
    const stId = String(route.query.station)
    store.selectStation(stId)
    const st = store.stations.find(s => s.id === stId)
    if (st?.plantationZone) {
      selectedZone.value = st.plantationZone
    }
    await loadSelectedStationHistory(stId)
    nextTick(() => {
      const el = document.getElementById('selected-station-deepdive')
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
        el.classList.add('ring-4', 'ring-cyan-500/60', 'transition-all', 'duration-500')
        setTimeout(() => {
          el.classList.remove('ring-4', 'ring-cyan-500/60')
        }, 2000)
      }
    })
  } else {
    await loadSelectedStationHistory()
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Top KPI Metrics Ribbon -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
      <!-- 1. Plantation Threat Status -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-3.5 shadow-lg flex flex-col justify-between">
        <div class="flex items-center justify-between text-slate-400 text-xs mb-1">
          <span>Infestation Threat</span>
          <ShieldAlert :size="15" :class="store.overallThreatLevel === 'Safe' ? 'text-emerald-400' : store.overallThreatLevel === 'Low' ? 'text-blue-400' : store.overallThreatLevel === 'Medium' ? 'text-amber-400' : 'text-rose-400'" />
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-xl font-black text-white">{{ store.overallThreatLevel }}</span>
          <span class="text-[10px] font-mono text-slate-400">ITI: {{ store.averageThreatIndex }}/100</span>
        </div>
        <div class="text-[11px] mt-1 font-medium" :class="store.overallThreatLevel === 'Safe' ? 'text-emerald-400' : store.overallThreatLevel === 'Low' ? 'text-blue-400' : store.overallThreatLevel === 'Medium' ? 'text-amber-400' : 'text-rose-400'">
          {{ store.overallThreatLevel === 'Safe' ? 'Rolling Window: Clean Baseline' : store.overallThreatLevel === 'High' ? 'Confirmed Biotic Foraging' : 'Multi-Cycle Temporal Watch' }}
        </div>
      </div>

      <!-- 2. Gated Duty Cycle -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-3.5 shadow-lg flex flex-col justify-between">
        <div class="flex items-center justify-between text-slate-400 text-xs mb-1">
          <span>Gated Sensing State</span>
          <Moon :size="15" class="text-indigo-400" />
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-xl font-black text-white">{{ store.dormantStationsCount }} Asleep</span>
          <span class="text-[10px] text-amber-400 font-semibold">{{ store.triggeredStationsCount }} Active</span>
        </div>
        <div class="text-[11px] text-slate-400 mt-1">Gated by 65% moisture</div>
      </div>

      <!-- 3. Reporting Cadence -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-3.5 shadow-lg flex flex-col justify-between">
        <div class="flex items-center justify-between text-slate-400 text-xs mb-1">
          <span>Telemetry Cadence</span>
          <Clock :size="15" class="text-cyan-400" />
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-xl font-black text-white font-mono">15m / 24h</span>
        </div>
        <div class="text-[11px] text-cyan-400 mt-1">15m Trigger &bull; 24h Alive Ping</div>
      </div>

      <!-- 4. Average Moisture -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-3.5 shadow-lg flex flex-col justify-between">
        <div class="flex items-center justify-between text-slate-400 text-xs mb-1">
          <span>Root-Zone Moisture</span>
          <Droplets :size="15" class="text-blue-400" />
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-xl font-black text-white font-mono">{{ store.averageMoisture }}%</span>
          <span class="text-[10px] text-slate-400">avg</span>
        </div>
        <div class="text-[11px] text-slate-400 mt-1">Trigger limit: &ge;65%</div>
      </div>

      <!-- 5. Detection Lead Time Advantage -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-3.5 shadow-lg flex flex-col justify-between">
        <div class="flex items-center justify-between text-slate-400 text-xs mb-1">
          <span>Detection Lead Time</span>
          <TrendingUp :size="15" class="text-emerald-400" />
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-xl font-black text-emerald-400 font-mono">+{{ store.metrics?.detectionLeadTimeDays || 42 }}d</span>
          <span class="text-[10px] text-slate-400">ahead</span>
        </div>
        <div class="text-[11px] text-slate-400 mt-1">vs quarterly manual check</div>
      </div>

      <!-- 6. Ultra-Low-Power Sleep Ratio -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-3.5 shadow-lg flex flex-col justify-between">
        <div class="flex items-center justify-between text-slate-400 text-xs mb-1">
          <span>Sleep Duty Ratio</span>
          <CheckCircle2 :size="15" class="text-emerald-400" />
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-xl font-black text-white font-mono">99.7%</span>
          <span class="text-[10px] text-emerald-400 font-semibold">Deep Sleep</span>
        </div>
        <div class="text-[11px] text-slate-400 mt-1">Zero LoRa traffic in sleep</div>
      </div>
    </div>

    <!-- Active SOP Warning Alert (if any active alerts) -->
    <div
      v-if="store.activeAlerts.length > 0"
      class="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4 shadow-xl flex flex-wrap items-center justify-between gap-4"
    >
      <div class="flex items-start gap-3 max-w-3xl">
        <div class="w-9 h-9 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center shrink-0 border border-amber-500/30">
          <AlertTriangle :size="20" />
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h4 class="font-bold text-white text-sm">Underground Moisture Trigger Breached &bull; SOP Required</h4>
            <span class="text-xs font-mono font-bold px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40">
              {{ store.activeAlerts[0].confidenceTier }} Confidence Tier
            </span>
          </div>
          <p class="text-xs text-slate-300 mt-1 leading-relaxed">
            {{ store.activeAlerts[0].summary }}
          </p>
        </div>
      </div>

      <button
        @click="handleOpenVerifyForAlert(store.activeAlerts[0])"
        class="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs flex items-center gap-2 transition shadow-lg shadow-amber-950/40"
      >
        <span>Execute Response Protocol ({{ store.activeAlerts[0].sopStage.toUpperCase() }})</span>
        <ArrowRight :size="14" />
      </button>
    </div>

    <!-- New IoT Station Auto-Discovery Notification Banner (Modeled after Response SOP & Alerts) -->
    <div
      v-if="store.unconfiguredStations.length > 0"
      class="space-y-3"
    >
      <div
        v-for="unconfiguredSt in store.unconfiguredStations"
        :key="unconfiguredSt.id"
        class="bg-indigo-950/30 border border-indigo-500/40 rounded-2xl p-4 shadow-xl flex flex-wrap items-center justify-between gap-4 transition-all"
      >
        <div class="flex items-start md:items-center gap-3.5">
          <div class="w-10 h-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center shrink-0 border border-indigo-500/40 shadow-inner mt-0.5 md:mt-0">
            <Radio :size="20" class="animate-pulse" />
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h4 class="font-bold text-white text-sm">
                New Station Go-Live Detected &bull; {{ unconfiguredSt.code }}
              </h4>
              <span class="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 text-[10px] font-bold border border-indigo-500/40 uppercase tracking-wider">
                Initial Packet Received
              </span>
              <!-- Live Continuous Monitoring Status & ITI -->
              <span
                class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border font-mono"
                :class="unconfiguredSt.threatLevel === 'High' ? 'bg-rose-500/20 text-rose-300 border-rose-500/40' : unconfiguredSt.threatLevel === 'Medium' ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' : 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40'"
              >
                Live ITI: {{ unconfiguredSt.threatIndex ?? 0 }}/100 ({{ unconfiguredSt.threatLevel || 'Monitoring' }})
              </span>
              <span class="text-xs font-mono text-cyan-300 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                Moisture: {{ unconfiguredSt.soilMoisture }}%
              </span>
              <span class="text-xs font-mono text-emerald-300 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                Battery: {{ unconfiguredSt.batteryLevel }}%
              </span>
            </div>
            <p class="text-xs text-slate-300 mt-1 leading-relaxed">
              Station <strong class="text-white">{{ unconfiguredSt.code }}</strong> transmitted its initial packet and joined the network. Telemetry monitoring and rolling Threat Index are already live with an empty profile. Please key in tree tag and plantation zone metadata to complete station assignment.
            </p>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="router.push({ path: '/events', query: { station: unconfiguredSt.id } })"
            class="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white font-semibold text-xs flex items-center gap-1.5 transition border border-slate-700 shadow"
            title="Open raw telemetry event records table"
          >
            <History :size="14" class="text-slate-400" />
            <span>Event Log</span>
          </button>

          <button
            @click="handleConfigureStation(unconfiguredSt)"
            class="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-2 transition shadow-lg shadow-indigo-950/50"
          >
            <Radio :size="14" />
            <span>Key In Station Profile</span>
            <ArrowRight :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- Main Grid: Map & Live Station Quick Overview -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- Left (7 Cols): Satellite Map & Station Cards -->
      <div class="lg:col-span-7 flex flex-col gap-4">
        <PlantationMap
          height-class="h-[480px]"
          @select-station="handleSelectStation"
          @configure-station="handleConfigureStation"
          @open-add-modal="handleOpenAddStation"
        />

        <!-- Station Selector Cards Header & Zone Dropdown Menu -->
        <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-3 shadow-lg flex flex-wrap items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-3">
            <div class="flex items-center gap-2">
              <MapPin :size="15" class="text-emerald-400" />
              <span class="text-xs font-bold text-white uppercase tracking-wider">Zone:</span>
            </div>

            <!-- Plantation Zone Dropdown Selector -->
            <div class="relative">
              <select
                :value="selectedZone"
                @change="handleZoneChange(($event.target as HTMLSelectElement).value)"
                class="bg-slate-950 border border-slate-700 hover:border-slate-600 focus:border-emerald-500 rounded-xl pl-3 pr-8 py-1.5 text-xs font-semibold text-white focus:outline-none focus:ring-1 focus:ring-emerald-500 cursor-pointer appearance-none shadow-inner"
              >
                <option value="all">All Plantation Zones ({{ store.stations.length }})</option>
                <option v-for="zone in availableZones" :key="zone" :value="zone">
                  {{ zone }} ({{ getZoneStationCount(zone) }})
                </option>
              </select>
              <ChevronDown :size="13" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            </div>

            <div class="flex items-center gap-2">
              <span class="text-[11px] text-slate-400 font-mono">
                Showing {{ filteredStations.length }} {{ filteredStations.length === 1 ? 'station' : 'stations' }}
              </span>
              <span v-if="store.unconfiguredStations.length > 0" class="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 text-[10px] font-bold border border-indigo-500/40">
                {{ store.unconfiguredStations.length }} New
              </span>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button
              @click="handleSimulateNewDiscovery"
              class="px-2.5 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 text-xs font-semibold flex items-center gap-1.5 transition shadow"
              title="Simulate a newly deployed IoT bait station transmitting its first LoRa packet"
            >
              <Radio :size="13" class="animate-pulse" />
              <span>Simulate New Go-Live</span>
            </button>
            <button
              @click="handleOpenVerify()"
              class="px-2.5 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-semibold flex items-center gap-1.5 transition shadow"
            >
              <ClipboardCheck :size="13" />
              <span>Log Field Audit</span>
            </button>
          </div>
        </div>

        <!-- Station Cards Grid (Filtered to display one zone at once) -->
        <div v-if="filteredStations.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <StationCard
            v-for="st in filteredStations"
            :key="st.id"
            :station="st"
            :is-selected="store.selectedStationId === st.id"
            @select="handleSelectStation"
            @edit="handleConfigureStation"
            @open-verify="handleOpenVerify"
          />
        </div>
        <div v-else class="p-8 text-center bg-slate-900/60 rounded-2xl border border-slate-800 text-slate-400 text-xs">
          No bait stations found in {{ selectedZone }}. Select another zone from the dropdown menu above.
        </div>
      </div>

      <!-- Right (5 Cols): Selected Station Deep-Dive -->
      <div id="selected-station-deepdive" class="lg:col-span-5 flex flex-col gap-4 rounded-2xl transition-all duration-300" v-if="store.selectedStation">
        <!-- Selected Station Header Panel -->
        <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-4 shadow-xl">
          <div class="flex items-center justify-between border-b border-slate-800 pb-3 mb-3">
            <div>
              <div class="flex items-center gap-2">
                <span class="text-base font-black text-white">{{ store.selectedStation.code }}</span>
                <span class="text-xs font-medium text-slate-400">({{ store.selectedStation.name }})</span>
                <span v-if="store.selectedStation.isProfileComplete === false" class="px-1.5 py-0.2 rounded bg-indigo-950 text-indigo-300 text-[9px] font-bold border border-indigo-700/60 uppercase">
                  New Go-Live
                </span>
              </div>
              <p class="text-xs font-mono">
                <template v-if="store.selectedStation.isProfileComplete === false">
                  <span class="text-indigo-400 font-semibold">Profile Incomplete &bull; {{ store.selectedStation.zone }}</span>
                  <button @click="handleConfigureStation(store.selectedStation)" class="ml-2 text-indigo-300 hover:text-white underline text-[11px] font-sans">
                    Key In Info &rarr;
                  </button>
                </template>
                <template v-else>
                  <span class="text-emerald-400">Tree: {{ store.selectedStation.treeTag }} &bull; {{ store.selectedStation.zone }}</span>
                </template>
              </p>
            </div>

            <div class="text-right">
              <span
                class="text-xs px-2.5 py-0.5 rounded-full font-bold border"
                :class="store.selectedStation.isAcousticGated ? 'bg-slate-800 text-slate-300 border-slate-700' : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'"
              >
                {{ store.selectedStation.isAcousticGated ? 'Acoustic Asleep (24h Ping)' : 'Acoustic Sampling (15m)' }}
              </span>
              <p class="text-[10px] text-slate-400 mt-1">{{ store.selectedStation.lastSeen }}</p>
            </div>
          </div>

          <!-- Gauges -->
          <div class="grid grid-cols-3 gap-2 text-center">
            <!-- Soil Moisture -->
            <div class="bg-slate-950 p-2.5 rounded-xl border" :class="store.selectedStation.soilMoisture >= store.selectedStation.moistureThreshold ? 'border-amber-500/40' : 'border-slate-800'">
              <span class="text-[10px] text-slate-400 block mb-0.5">Soil Moisture</span>
              <span class="text-lg font-bold font-mono text-cyan-300">{{ store.selectedStation.soilMoisture }}%</span>
              <span class="text-[9px] block" :class="store.selectedStation.soilMoisture >= store.selectedStation.moistureThreshold ? 'text-amber-400 font-semibold' : 'text-slate-500'">
                {{ store.selectedStation.soilMoisture >= store.selectedStation.moistureThreshold ? 'Triggered &ge;65%' : '< 65% (Gated)' }}
              </span>
            </div>

            <!-- Acoustic State -->
            <div class="bg-slate-950 p-2.5 rounded-xl border border-slate-800">
              <span class="text-[10px] text-slate-400 block mb-0.5">Acoustic Score</span>
              <span v-if="!store.selectedStation.isAcousticGated" class="text-lg font-bold font-mono text-amber-400">
                {{ store.selectedStation.activityScore }}/100
              </span>
              <span v-else class="text-xs font-bold font-mono text-indigo-300 block mt-1.5">
                DEEP SLEEP
              </span>
              <span class="text-[9px] text-slate-500 block">
                {{ store.selectedStation.isAcousticGated ? '0 µA Standby' : '15m Window' }}
              </span>
            </div>

            <!-- Cadence / Schedule -->
            <div class="bg-slate-950 p-2.5 rounded-xl border border-slate-800">
              <span class="text-[10px] text-slate-400 block mb-0.5">Next Packet</span>
              <span class="text-xs font-bold font-mono text-slate-200 mt-1 block">
                {{ store.selectedStation.isAcousticGated ? 'Daily (24h)' : '15m Window' }}
              </span>
              <span class="text-[9px] text-slate-500 block">LoRa Uplink</span>
            </div>
          </div>
        </div>

        <!-- Telemetry Time Series Chart -->
        <TelemetryChart
          :records="stationHistory"
          :station-code="store.selectedStation.code"
          :is-acoustic-gated="store.selectedStation.isAcousticGated"
        />

        <!-- MFCC Spectral & Softmax Classifier -->
        <MfccVisualizer
          :mfcc-features="store.selectedStation.mfccFeatures"
          :activity-score="store.selectedStation.activityScore"
          :confidence-tier="store.selectedStation.confidenceTier"
          :is-acoustic-gated="store.selectedStation.isAcousticGated"
          :soil-moisture="store.selectedStation.soilMoisture"
          :moisture-threshold="store.selectedStation.moistureThreshold"
          @wake-acoustic="handleWakeAcoustic"
        />

        <!-- Live Telemetry Feed (15m Events & Daily Keepalives via WebSocket) -->
        <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-4 shadow-xl">
          <div class="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
            <div class="flex items-center gap-2">
              <Radio :size="15" class="text-emerald-400" />
              <h4 class="text-xs font-bold text-white uppercase tracking-wider">WebSocket Feed (/ws/telemetry)</h4>
            </div>
            <span class="text-[10px] text-slate-400">15m Triggered &bull; Daily Heartbeats</span>
          </div>

          <div class="space-y-1.5 max-h-48 overflow-y-auto pr-1 text-[11px] font-mono">
            <div
              v-for="pkt in store.livePackets.slice(0, 5)"
              :key="pkt.id"
              class="flex items-center justify-between p-2 rounded-lg bg-slate-950 border border-slate-800/80 hover:border-slate-700"
            >
              <div class="flex items-center gap-2">
                <span
                  class="w-2 h-2 rounded-full"
                  :class="(pkt.eventType || pkt.transmissionType) === '15min_triggered_event' ? 'bg-amber-400 animate-ping' : 'bg-emerald-400'"
                ></span>
                <span class="text-slate-400">{{ pkt.timestamp.split(' ')[1] }}</span>
                <span class="text-white font-bold">{{ pkt.stationId }}</span>
              </div>

              <div class="flex items-center gap-2.5">
                <span class="text-cyan-400">{{ pkt.soilMoisture }}%</span>

                <span v-if="(pkt.eventType || pkt.transmissionType) === '15min_triggered_event'" class="text-amber-400 font-bold">
                  Score: {{ pkt.acousticScore ?? pkt.activityScore }}
                </span>
                <span v-else class="text-slate-500 italic">
                  Bat: {{ pkt.batteryLevel }}%
                </span>

                <span
                  class="text-[10px] px-1.5 py-0.2 rounded font-sans font-bold"
                  :class="(pkt.eventType || pkt.transmissionType) === '15min_triggered_event' ? 'bg-amber-950 text-amber-300 border border-amber-800' : 'bg-slate-800 text-slate-300'"
                >
                  {{ (pkt.eventType || pkt.transmissionType) === '15min_triggered_event' ? '15m TRIG' : 'DAILY PING' }}
                </span>
              </div>
            </div>

            <div v-if="store.livePackets.length === 0" class="text-slate-500 text-center py-4 text-xs font-sans">
              Waiting for 15-min triggered packets or daily alive heartbeats...
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->
    <StationModal
      :is-open="isStationModalOpen"
      :station-to-edit="stationToEdit"
      @close="isStationModalOpen = false; stationToEdit = null"
      @save="handleSaveStation"
    />

    <FieldVerificationModal
      :is-open="isVerifyModalOpen"
      :preselected-station-id="stationForVerify"
      @close="isVerifyModalOpen = false"
    />
  </div>
</template>
