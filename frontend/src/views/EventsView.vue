<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePlantationStore } from '@/stores/plantationStore'
import type { StationEvent, PredictLevel, EventType } from '@/types'
import {
  History,
  Search,
  Filter,
  Calendar,
  Clock,
  Radio,
  Activity,
  Droplets,
  Battery,
  BatteryCharging,
  Zap,
  Moon,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  X,
  SlidersHorizontal,
  RotateCcw,
  Layers,
  Thermometer,
  ShieldAlert,
  Signal,
  ClipboardCheck
} from '@lucide/vue'

const store = usePlantationStore()
const route = useRoute()

// Filter states
const stationSearchInput = ref('')
const selectedStationId = ref<string>('all')
const isStationDropdownOpen = ref(false)

const selectedEventType = ref<string>('all')
const selectedPredictLevel = ref<string>('all')
const selectedGroundTruth = ref<string>('all')

const startDate = ref<string>('')
const endDate = ref<string>('')
const activeTimeShortcut = ref<string>('all')

// Expanded MFCC details modal/drawer state
const expandedEvent = ref<StationEvent | null>(null)

// Pagination
const currentPage = ref(1)
const itemsPerPage = ref(12)

// Station search dropdown filtering
const filteredStationOptions = computed(() => {
  const query = stationSearchInput.value.toLowerCase().trim()
  if (!query) return store.stations
  return store.stations.filter(s =>
    s.code.toLowerCase().includes(query) ||
    s.name.toLowerCase().includes(query) ||
    s.id.toLowerCase().includes(query) ||
    s.zone.toLowerCase().includes(query) ||
    s.treeTag.toLowerCase().includes(query)
  )
})

const selectedStationName = computed(() => {
  if (selectedStationId.value === 'all') return 'All Stations'
  const st = store.stations.find(s => s.id === selectedStationId.value)
  return st ? `${st.code} - ${st.name}` : selectedStationId.value
})

function selectStation(stationId: string, stationLabel?: string) {
  selectedStationId.value = stationId
  stationSearchInput.value = stationId === 'all' ? '' : (stationLabel || stationId)
  isStationDropdownOpen.value = false
  currentPage.value = 1
}

function clearStationSelection() {
  selectedStationId.value = 'all'
  stationSearchInput.value = ''
  currentPage.value = 1
}

// Time shortcuts: 24h, 7 days, 30 days, all
function setTimeShortcut(shortcut: '24h' | '7d' | '30d' | 'all') {
  activeTimeShortcut.value = shortcut
  currentPage.value = 1
  const now = new Date()

  if (shortcut === 'all') {
    startDate.value = ''
    endDate.value = ''
    return
  }

  // Current date formatted as YYYY-MM-DD
  endDate.value = now.toISOString().split('T')[0]

  const pastDate = new Date()
  if (shortcut === '24h') {
    pastDate.setDate(now.getDate() - 1)
  } else if (shortcut === '7d') {
    pastDate.setDate(now.getDate() - 7)
  } else if (shortcut === '30d') {
    pastDate.setDate(now.getDate() - 30)
  }
  startDate.value = pastDate.toISOString().split('T')[0]
}

function resetAllFilters() {
  selectedStationId.value = 'all'
  stationSearchInput.value = ''
  selectedEventType.value = 'all'
  selectedPredictLevel.value = 'all'
  selectedGroundTruth.value = 'all'
  startDate.value = ''
  endDate.value = ''
  activeTimeShortcut.value = 'all'
  currentPage.value = 1
}

// Main Events Filtering
const filteredEvents = computed(() => {
  return store.allEvents.filter(event => {
    // 1. Station ID Filter
    if (selectedStationId.value !== 'all' && event.stationId !== selectedStationId.value) {
      return false
    }

    // 2. Event Type Filter
    if (selectedEventType.value !== 'all' && event.eventType !== selectedEventType.value) {
      return false
    }

    // 3. Prediction Level Filter
    if (selectedPredictLevel.value !== 'all' && event.predictLevel !== selectedPredictLevel.value) {
      return false
    }

    // 4. Ground Truth Validation Filter
    if (selectedGroundTruth.value !== 'all') {
      if (selectedGroundTruth.value === 'unlabeled') {
        if (event.validationOutcome) return false
      } else if (event.validationOutcome !== selectedGroundTruth.value) {
        return false
      }
    }

    // 5. Date Range Filter
    if (startDate.value) {
      const eventDate = event.timestamp.split(' ')[0]
      if (eventDate < startDate.value) return false
    }
    if (endDate.value) {
      const eventDate = event.timestamp.split(' ')[0]
      if (eventDate > endDate.value) return false
    }

    return true
  })
})

// Counts breakdown
const totalFilteredCount = computed(() => filteredEvents.value.length)
const countTriggered = computed(() => filteredEvents.value.filter(e => e.eventType === '15min_triggered_event').length)
const countHeartbeat = computed(() => filteredEvents.value.filter(e => e.eventType === 'daily_heartbeat').length)
const countHigh = computed(() => filteredEvents.value.filter(e => e.predictLevel === 'High').length)
const countMedium = computed(() => filteredEvents.value.filter(e => e.predictLevel === 'Medium').length)

// Pagination slicing
const totalPages = computed(() => Math.max(1, Math.ceil(filteredEvents.value.length / itemsPerPage.value)))
const paginatedEvents = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value
  return filteredEvents.value.slice(start, start + itemsPerPage.value)
})

function getStationMeta(stationId: string) {
  const st = store.stations.find(s => s.id === stationId)
  return {
    code: st?.code || st?.id || stationId,
    name: st?.name || (st?.isProfileComplete === false ? 'Awaiting Profile Setup' : 'In-Ground Station'),
    zone: st?.zone || st?.plantationZone || (st?.isProfileComplete === false ? 'Unassigned Zone' : 'Plantation Zone'),
    treeTag: st?.treeTag || (st?.isProfileComplete === false ? 'Pending Setup' : 'PW-RMB')
  }
}

function getTierBadgeClass(level: PredictLevel) {
  switch (level) {
    case 'High':
      return 'bg-rose-500/20 text-rose-300 border-rose-500/40'
    case 'Medium':
      return 'bg-amber-500/20 text-amber-300 border-amber-500/40'
    case 'Low':
      return 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40'
    default:
      return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
  }
}

onMounted(async () => {
  if (route.query.station) {
    selectStation(String(route.query.station))
  }
  if (store.allEvents.length === 0) {
    await store.loadAllEvents()
  }
})

watch(() => route.query.station, (newSt) => {
  if (newSt) {
    selectStation(String(newSt))
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- View Header -->
    <div class="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 flex flex-wrap items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-xl bg-cyan-500/20 text-cyan-400 flex items-center justify-center border border-cyan-500/30">
            <History :size="20" />
          </div>
          <div>
            <h2 class="text-lg font-black text-white flex items-center gap-2">
              <span>Event Table Log & Telemetry Records</span>
              <span class="text-xs font-mono font-bold px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-800">
                Database Table 2: events
              </span>
            </h2>
            <p class="text-xs text-slate-400">Time-series telemetry audit &bull; Monitored in real-time by WebSocket</p>
          </div>
        </div>
      </div>

      <!-- Quick Metrics Counters & Ground Truth Badges -->
      <div class="flex flex-wrap items-center gap-2 text-xs">
        <span class="px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 font-mono">
          Total: <strong class="text-white">{{ totalFilteredCount }}</strong> records
        </span>
        <span class="px-2.5 py-1.5 rounded-xl bg-amber-950/60 border border-amber-800/80 text-amber-300 font-mono">
          {{ countTriggered }} Triggered (15m)
        </span>
        <span class="px-2.5 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-400 font-mono">
          {{ countHeartbeat }} Heartbeats (24h)
        </span>
        <!-- Ground Truth Mini Pill -->
        <span v-if="store.groundTruthStats.total > 0" class="px-2.5 py-1.5 rounded-xl bg-emerald-950/60 border border-emerald-800/80 text-emerald-300 font-mono flex items-center gap-1.5">
          <ClipboardCheck :size="12" class="text-emerald-400" />
          <span>{{ store.groundTruthStats.total }} Labeled:</span>
          <span class="text-[10px] text-emerald-400 font-bold">({{ store.groundTruthStats.tp }} TP &bull; {{ store.groundTruthStats.fp }} FP &bull; {{ store.groundTruthStats.fn }} FN &bull; {{ store.groundTruthStats.tn }} TN)</span>
        </span>
      </div>
    </div>

    <!-- Filter Control Panel -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-5 shadow-xl space-y-4">
      <div class="flex items-center justify-between border-b border-slate-800 pb-3">
        <div class="flex items-center gap-2 text-sm font-bold text-white">
          <SlidersHorizontal :size="16" class="text-emerald-400" />
          <span>Search & Telemetry Filters</span>
        </div>

        <button
          @click="resetAllFilters"
          class="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 transition"
        >
          <RotateCcw :size="13" />
          <span>Reset Filters</span>
        </button>
      </div>

      <!-- 5 Filter Inputs Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-3 text-xs">
        <!-- 1. Station ID Search with Dynamic Autocomplete Dropdown -->
        <div class="relative">
          <label class="block text-slate-300 font-semibold mb-1.5 flex items-center gap-1.5">
            <Search :size="13" class="text-emerald-400" />
            <span>Station ID / Name</span>
          </label>

          <div class="relative">
            <input
              v-model="stationSearchInput"
              type="text"
              placeholder="Search station or select..."
              @focus="isStationDropdownOpen = true"
              class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white outline-none focus:border-emerald-500 pr-8 font-sans"
            />
            <button
              v-if="stationSearchInput || selectedStationId !== 'all'"
              @click="clearStationSelection"
              class="absolute right-2.5 top-2.5 text-slate-500 hover:text-white"
            >
              <X :size="14" />
            </button>
            <ChevronDown
              v-else
              :size="14"
              class="absolute right-2.5 top-2.5 text-slate-500 pointer-events-none"
            />
          </div>

          <!-- Dynamic Autocomplete Dropdown Menu -->
          <div
            v-if="isStationDropdownOpen"
            class="absolute left-0 right-0 top-full mt-1 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl z-50 max-h-56 overflow-y-auto p-1 text-xs"
          >
            <!-- All Stations Option -->
            <div
              @click="selectStation('all', 'All Stations')"
              class="p-2 rounded-lg cursor-pointer hover:bg-slate-800 flex items-center justify-between transition"
              :class="selectedStationId === 'all' ? 'bg-emerald-500/10 text-emerald-300 font-bold' : 'text-slate-300'"
            >
              <span>All Stations (Show Everything)</span>
              <span v-if="selectedStationId === 'all'" class="text-emerald-400">✓</span>
            </div>

            <!-- Filtered Stations -->
            <div
              v-for="st in filteredStationOptions"
              :key="st.id"
              @click="selectStation(st.id, `${st.code} - ${st.name}`)"
              class="p-2 rounded-lg cursor-pointer hover:bg-slate-800 flex items-center justify-between transition"
              :class="selectedStationId === st.id ? 'bg-emerald-500/10 text-emerald-300 font-bold' : 'text-slate-300'"
            >
              <div>
                <div class="font-bold text-white flex items-center gap-1.5">
                  <span>{{ st.code }}</span>
                  <span class="text-[10px] text-slate-400 font-normal">({{ st.treeTag }})</span>
                </div>
                <div class="text-[10px] text-slate-400">{{ st.name }} &bull; {{ st.zone }}</div>
              </div>
              <span v-if="selectedStationId === st.id" class="text-emerald-400">✓</span>
            </div>

            <div v-if="filteredStationOptions.length === 0" class="p-3 text-center text-slate-500 text-[11px]">
              No stations match "{{ stationSearchInput }}"
            </div>
          </div>
        </div>

        <!-- 2. Event Type Dropdown Menu -->
        <div>
          <label class="block text-slate-300 font-semibold mb-1.5 flex items-center gap-1.5">
            <Radio :size="13" class="text-amber-400" />
            <span>Event Type</span>
          </label>
          <select
            v-model="selectedEventType"
            @change="currentPage = 1"
            class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white outline-none focus:border-amber-500"
          >
            <option value="all">All Event Types</option>
            <option value="15min_triggered_event">⚡ 15-min Triggered Acoustic Sampling</option>
            <option value="daily_heartbeat">💤 24-hour Daily Keepalive Heartbeat</option>
          </select>
        </div>

        <!-- 3. Prediction Level Dropdown Menu -->
        <div>
          <label class="block text-slate-300 font-semibold mb-1.5 flex items-center gap-1.5">
            <ShieldAlert :size="13" class="text-rose-400" />
            <span>Prediction Level</span>
          </label>
          <select
            v-model="selectedPredictLevel"
            @change="currentPage = 1"
            class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white outline-none focus:border-rose-500"
          >
            <option value="all">All Prediction Levels</option>
            <option value="High">🔴 High Escalation</option>
            <option value="Medium">🟠 Medium Alert</option>
            <option value="Low">🔵 Low Risk</option>
            <option value="Safe">🟢 Safe / Baseline</option>
          </select>
        </div>

        <!-- 4. Ground Truth Validation Dropdown -->
        <div>
          <label class="block text-slate-300 font-semibold mb-1.5 flex items-center gap-1.5">
            <ClipboardCheck :size="13" class="text-emerald-400" />
            <span>Ground Truth (HITL)</span>
          </label>
          <select
            v-model="selectedGroundTruth"
            @change="currentPage = 1"
            class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white outline-none focus:border-emerald-500"
          >
            <option value="all">All Telemetry Records</option>
            <option value="TP">🟢 TP: True Positive (Confirmed)</option>
            <option value="FP">🔴 FP: False Positive (False Alarm)</option>
            <option value="FN">🟣 FN: False Negative (Missed)</option>
            <option value="TN">🔵 TN: True Negative (Clean Pass)</option>
            <option value="unlabeled">⚪ Unlabeled Telemetry</option>
          </select>
        </div>

        <!-- 5. Date Range Calendar Selection -->
        <div>
          <label class="block text-slate-300 font-semibold mb-1.5 flex items-center gap-1.5">
            <Calendar :size="13" class="text-cyan-400" />
            <span>Time Range</span>
          </label>
          <div class="flex items-center gap-1.5">
            <input
              v-model="startDate"
              type="date"
              @change="activeTimeShortcut = 'custom'; currentPage = 1"
              class="w-1/2 bg-slate-950 border border-slate-800 rounded-xl px-2 py-1.5 text-white outline-none focus:border-cyan-500 font-mono text-[10px]"
              title="Start Date"
            />
            <span class="text-slate-500 text-[10px]">to</span>
            <input
              v-model="endDate"
              type="date"
              @change="activeTimeShortcut = 'custom'; currentPage = 1"
              class="w-1/2 bg-slate-950 border border-slate-800 rounded-xl px-2 py-1.5 text-white outline-none focus:border-cyan-500 font-mono text-[10px]"
              title="End Date"
            />
          </div>
        </div>
      </div>

      <!-- Quick Date Range Shortcuts -->
      <div class="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/60">
        <div class="flex items-center gap-2 text-xs">
          <span class="text-slate-400 text-[11px] font-medium">Quick Shortcuts:</span>
          <button
            @click="setTimeShortcut('24h')"
            class="px-2.5 py-1 rounded-lg text-[11px] font-medium transition"
            :class="activeTimeShortcut === '24h' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 font-bold' : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'"
          >
            Last 24 Hours
          </button>
          <button
            @click="setTimeShortcut('7d')"
            class="px-2.5 py-1 rounded-lg text-[11px] font-medium transition"
            :class="activeTimeShortcut === '7d' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 font-bold' : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'"
          >
            Recent 7 Days
          </button>
          <button
            @click="setTimeShortcut('30d')"
            class="px-2.5 py-1 rounded-lg text-[11px] font-medium transition"
            :class="activeTimeShortcut === '30d' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 font-bold' : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'"
          >
            Recent 30 Days
          </button>
          <button
            @click="setTimeShortcut('all')"
            class="px-2.5 py-1 rounded-lg text-[11px] font-medium transition"
            :class="activeTimeShortcut === 'all' ? 'bg-slate-800 text-white font-bold' : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'"
          >
            All Time
          </button>
        </div>

        <!-- Filter status active label -->
        <div class="text-[11px] text-slate-400">
          Filtered Station: <strong class="text-white">{{ selectedStationName }}</strong>
        </div>
      </div>
    </div>

    <!-- Event Cards Grid -->
    <div v-if="paginatedEvents.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="evt in paginatedEvents"
        :key="evt.id"
        class="bg-slate-900/90 rounded-2xl border p-4 shadow-xl transition-all duration-200 hover:border-slate-700 flex flex-col justify-between"
        :class="evt.predictLevel === 'High' ? 'border-rose-500/40 bg-slate-900 ring-1 ring-rose-500/20' : evt.predictLevel === 'Medium' ? 'border-amber-500/40' : 'border-slate-800'"
      >
        <!-- Card Top Header -->
        <div>
          <div class="flex items-start justify-between gap-2 mb-2 pb-2 border-b border-slate-800">
            <div>
              <div class="flex items-center gap-1.5">
                <span class="font-black text-white text-sm">
                  {{ getStationMeta(evt.stationId).code }}
                </span>
                <span class="text-[10px] text-emerald-400 font-mono">
                  {{ getStationMeta(evt.stationId).treeTag }}
                </span>
              </div>
              <div class="text-[11px] text-slate-400">
                {{ getStationMeta(evt.stationId).name }}
              </div>
            </div>

            <!-- Predict Level Badge -->
            <span
              class="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border"
              :class="getTierBadgeClass(evt.predictLevel)"
            >
              {{ evt.predictLevel }} Tier
            </span>
          </div>

          <!-- Event Type & Timestamp -->
          <div class="flex items-center justify-between text-xs mb-3">
            <span
              class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold"
              :class="evt.eventType === '15min_triggered_event' ? 'bg-amber-950 text-amber-300 border border-amber-800' : 'bg-slate-800 text-slate-300'"
            >
              <component :is="evt.eventType === '15min_triggered_event' ? Zap : Moon" :size="11" />
              <span>{{ evt.eventType === '15min_triggered_event' ? '15m Triggered Event' : '24h Daily Keepalive' }}</span>
            </span>

            <span class="text-[11px] text-slate-400 font-mono flex items-center gap-1">
              <Clock :size="11" />
              <span>{{ evt.timestamp }}</span>
            </span>
          </div>

          <!-- Ground-Truth Validation Overlay Tag -->
          <div v-if="evt.validationOutcome" class="mb-3 px-2 py-1.5 rounded-lg border flex items-center justify-between text-[10px]"
            :class="evt.validationOutcome === 'TP' ? 'bg-emerald-950/60 border-emerald-500/40 text-emerald-300' : evt.validationOutcome === 'FP' ? 'bg-rose-950/60 border-rose-500/40 text-rose-300' : evt.validationOutcome === 'FN' ? 'bg-purple-950/60 border-purple-500/40 text-purple-300' : 'bg-blue-950/60 border-blue-500/40 text-blue-300'">
            <div class="flex items-center gap-1.5 font-bold">
              <span class="px-1.5 py-0.2 rounded font-mono text-[9px]" :class="evt.validationOutcome === 'TP' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : evt.validationOutcome === 'FP' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' : evt.validationOutcome === 'FN' ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30' : 'bg-blue-500/20 text-blue-300 border border-blue-500/30'">
                {{ evt.validationOutcome }}
              </span>
              <span>
                {{ evt.validationOutcome === 'TP' ? 'True Positive (Confirmed Infestation)' : evt.validationOutcome === 'FP' ? 'False Positive (Benign False Alarm)' : evt.validationOutcome === 'FN' ? 'False Negative (Unflagged Infestation)' : 'True Negative (Clean Tree Patrol)' }}
              </span>
            </div>
            <span v-if="evt.validatedBy" class="text-[9px] text-slate-400">by {{ evt.validatedBy.split(' ')[0] }}</span>
          </div>
          <div v-else class="mb-3 px-2 py-1 rounded-lg border border-slate-800/80 bg-slate-950/50 flex items-center justify-between text-[10px] text-slate-500">
            <span class="flex items-center gap-1.5 font-mono text-[9px] text-slate-400">
              <span class="w-1.5 h-1.5 rounded-full bg-slate-600"></span>
              <span>Ground Truth: Pending Human Audit</span>
            </span>
            <span class="text-[9px] italic text-slate-500 font-mono">Unlabeled</span>
          </div>

          <!-- Main Sensor Reading Gauges -->
          <div class="grid grid-cols-2 gap-2 mb-3">
            <!-- Soil Moisture -->
            <div
              class="bg-slate-950/80 p-2.5 rounded-xl border text-xs"
              :class="evt.soilMoisture >= 65 ? 'border-amber-500/30' : 'border-slate-800'"
            >
              <div class="flex items-center justify-between text-[11px] text-slate-400 mb-0.5">
                <span class="flex items-center gap-1"><Droplets :size="11" class="text-cyan-400" /> Soil Moisture</span>
                <span class="font-bold font-mono text-cyan-300">{{ evt.soilMoisture }}%</span>
              </div>
              <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full"
                  :class="evt.soilMoisture >= 65 ? 'bg-amber-500' : 'bg-cyan-500'"
                  :style="{ width: `${evt.soilMoisture}%` }"
                ></div>
              </div>
              <div class="text-[9px] text-slate-500 mt-1 flex justify-between">
                <span>Trg: 65%</span>
                <span :class="evt.soilMoisture >= 65 ? 'text-amber-400 font-bold' : 'text-slate-500'">
                  {{ evt.soilMoisture >= 65 ? 'BREACHED' : 'Normal' }}
                </span>
              </div>
            </div>

            <!-- Acoustic Score -->
            <div class="bg-slate-950/80 p-2.5 rounded-xl border border-slate-800 text-xs">
              <div class="flex items-center justify-between text-[11px] text-slate-400 mb-0.5">
                <span class="flex items-center gap-1"><Activity :size="11" class="text-amber-400" /> Acoustic (15m)</span>
                <span v-if="evt.acousticScore !== null" class="font-bold font-mono text-amber-400">
                  {{ evt.acousticScore }}/100
                </span>
                <span v-else class="text-[9px] font-mono text-indigo-300 font-semibold">
                  DEEP SLEEP
                </span>
              </div>

              <div v-if="evt.acousticScore !== null" class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full"
                  :class="evt.acousticScore > 65 ? 'bg-rose-500' : 'bg-amber-500'"
                  :style="{ width: `${evt.acousticScore}%` }"
                ></div>
              </div>
              <div v-else class="text-[9px] text-slate-500 italic mt-1">
                Gated 0 &mu;A Standby
              </div>

              <div v-if="evt.acousticScore !== null" class="text-[9px] text-amber-400 font-medium mt-1">
                Risk Score: {{ evt.aiRiskScore }}%
              </div>
            </div>
          </div>
        </div>

        <!-- Card Footer Info -->
        <div class="pt-2 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
          <div class="flex items-center gap-2">
            <!-- Microclimate -->
            <span class="flex items-center gap-1 font-mono text-[10px]">
              <Thermometer :size="11" class="text-rose-400" />
              {{ evt.temperature }}°C / {{ evt.humidity }}%
            </span>

            <!-- Battery -->
            <span class="flex items-center gap-1 font-mono text-[10px]">
              <Battery :size="11" class="text-emerald-400" />
              {{ evt.batteryLevel }}% ({{ evt.batteryVoltage }}V)
            </span>
          </div>

          <!-- MFCC or Event ID tag -->
          <div class="flex items-center gap-1.5">
            <button
              v-if="evt.mfccFeatures && evt.mfccFeatures.length"
              @click="expandedEvent = evt"
              class="px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-800 font-mono text-[10px] hover:bg-cyan-900 transition"
              title="Inspect 13-band MFCC Spectrogram"
            >
              MFCC
            </button>
            <span class="font-mono text-[9px] text-slate-500">{{ evt.id.split('-').slice(-2).join('-') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div
      v-else
      class="bg-slate-900/60 rounded-2xl border border-slate-800 p-12 text-center space-y-3"
    >
      <div class="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center text-slate-400 mx-auto">
        <Filter :size="22" />
      </div>
      <h3 class="text-base font-bold text-white">No Matching Events Found</h3>
      <p class="text-xs text-slate-400 max-w-sm mx-auto">
        No records match the current filter criteria (Station: {{ selectedStationName }}, Type: {{ selectedEventType }}, Tier: {{ selectedPredictLevel }}).
      </p>
      <button
        @click="resetAllFilters"
        class="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs transition"
      >
        Clear All Filters
      </button>
    </div>

    <!-- Pagination Controls -->
    <div
      v-if="totalPages > 1"
      class="bg-slate-900/80 p-4 rounded-2xl border border-slate-800 flex flex-wrap items-center justify-between gap-4 text-xs"
    >
      <div class="text-slate-400">
        Showing <strong class="text-white">{{ (currentPage - 1) * itemsPerPage + 1 }}</strong> to
        <strong class="text-white">{{ Math.min(currentPage * itemsPerPage, totalFilteredCount) }}</strong> of
        <strong class="text-white">{{ totalFilteredCount }}</strong> events
      </div>

      <div class="flex items-center gap-1.5">
        <button
          @click="currentPage = Math.max(1, currentPage - 1)"
          :disabled="currentPage === 1"
          class="p-2 rounded-lg bg-slate-800 text-slate-300 disabled:opacity-40 disabled:pointer-events-none hover:bg-slate-700 transition"
        >
          <ChevronLeft :size="14" />
        </button>

        <span class="px-3 py-1 bg-slate-950 rounded-lg border border-slate-800 font-mono text-white">
          Page {{ currentPage }} of {{ totalPages }}
        </span>

        <button
          @click="currentPage = Math.min(totalPages, currentPage + 1)"
          :disabled="currentPage === totalPages"
          class="p-2 rounded-lg bg-slate-800 text-slate-300 disabled:opacity-40 disabled:pointer-events-none hover:bg-slate-700 transition"
        >
          <ChevronRight :size="14" />
        </button>
      </div>
    </div>

    <!-- MFCC Inspection Modal -->
    <div
      v-if="expandedEvent"
      class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm"
    >
      <div class="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg shadow-2xl p-6 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h3 class="font-bold text-white text-sm">13-Band MFCC Spectral Fingerprint</h3>
            <p class="text-[11px] text-slate-400 font-mono">Event {{ expandedEvent.id }} &bull; {{ expandedEvent.timestamp }}</p>
          </div>
          <button @click="expandedEvent = null" class="text-slate-400 hover:text-white p-1 rounded-lg">
            <X :size="16" />
          </button>
        </div>

        <div class="grid grid-cols-13 gap-1 h-24 items-end bg-slate-950 p-2.5 rounded-xl border border-slate-800">
          <div
            v-for="(val, idx) in expandedEvent.mfccFeatures"
            :key="idx"
            class="flex flex-col items-center justify-end h-full relative group"
          >
            <div
              class="w-full rounded-t transition-all"
              :style="{
                height: `${Math.min(100, Math.max(15, (val + 25) * 2))}%`,
                backgroundColor: val > 6 ? '#ef4444' : val > 0 ? '#06b6d4' : '#64748b'
              }"
            ></div>
            <span class="text-[8px] font-mono text-slate-400 mt-1">{{ idx }}</span>
          </div>
        </div>

        <div class="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs space-y-1 font-mono text-slate-300">
          <div>Station: <strong class="text-white">{{ getStationMeta(expandedEvent.stationId).code }} ({{ expandedEvent.stationId }})</strong></div>
          <div>Acoustic Score: <strong class="text-amber-400">{{ expandedEvent.acousticScore }}/100</strong></div>
          <div>Prediction Tier: <strong class="text-rose-400">{{ expandedEvent.predictLevel }}</strong> (AI Risk: {{ expandedEvent.aiRiskScore }}%)</div>
        </div>

        <div class="flex justify-end">
          <button
            @click="expandedEvent = null"
            class="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold"
          >
            Close
          </button>
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
