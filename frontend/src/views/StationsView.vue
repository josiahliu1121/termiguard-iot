<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePlantationStore } from '@/stores/plantationStore'
import PlantationMap from '@/components/PlantationMap.vue'
import StationModal from '@/components/StationModal.vue'
import FieldVerificationModal from '@/components/FieldVerificationModal.vue'
import { formatCleanTimeAgo } from '@/mocks/mockData'
import type { Station } from '@/types'
import {
  MapPin,
  Plus,
  Radio,
  Sun,
  Battery,
  BatteryCharging,
  Signal,
  Edit,
  Trees,
  CheckCircle2,
  AlertTriangle,
  Moon,
  Clock,
  Activity,
  ArrowRight,
  History,
  ClipboardCheck,
  ShieldAlert,
  Droplets,
  Thermometer,
  Zap,
  Compass,
  Search,
  X,
  ChevronDown,
  RotateCcw,
  Filter,
  Layers
} from '@lucide/vue'

const store = usePlantationStore()
const router = useRouter()
const isModalOpen = ref(false)
const stationToEdit = ref<Station | null>(null)
const isVerifyModalOpen = ref(false)
const stationForVerify = ref<string>('')

// Filter & Dynamic Search State
const searchQuery = ref<string>('')
const isSearchDropdownOpen = ref<boolean>(false)
const searchContainerRef = ref<HTMLElement | null>(null)
const selectedZone = ref<string>('all')
const selectedThreat = ref<string>('all')
const selectedOperatingState = ref<string>('all')
const selectedAuditStatus = ref<string>('all')

// Helper for normalized plantation zone (guarantees unconfigured stations map to Unassigned)
function getStationZone(station: Station): string {
  if (!station.plantationZone || station.plantationZone.trim() === '' || station.isProfileComplete === false || station.plantationZone === 'Unassigned (Pending Setup)') {
    return 'Unassigned (Pending Setup)'
  }
  return station.plantationZone
}

// Unique list of available plantation zones
const availableZones = computed(() => {
  const zones = new Set<string>()
  store.stations.forEach(s => {
    zones.add(getStationZone(s))
  })
  return Array.from(zones).sort((a, b) => {
    if (a.startsWith('Unassigned')) return 1
    if (b.startsWith('Unassigned')) return -1
    return a.localeCompare(b)
  })
})

function getZoneStationCount(zone: string): number {
  return store.stations.filter(s => getStationZone(s) === zone).length
}

// Dynamic autocomplete search suggestions
const searchSuggestions = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return store.stations
  return store.stations.filter(s =>
    s.code.toLowerCase().includes(q) ||
    s.id.toLowerCase().includes(q) ||
    s.name.toLowerCase().includes(q) ||
    (s.treeTag && s.treeTag.toLowerCase().includes(q)) ||
    getStationZone(s).toLowerCase().includes(q)
  )
})

// Unified multi-criteria filtered stations
const filteredStations = computed(() => {
  return store.stations.filter(s => {
    // 1. Search Query Filter
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.trim().toLowerCase()
      const match =
        s.code.toLowerCase().includes(q) ||
        s.id.toLowerCase().includes(q) ||
        s.name.toLowerCase().includes(q) ||
        (s.treeTag && s.treeTag.toLowerCase().includes(q)) ||
        getStationZone(s).toLowerCase().includes(q)
      if (!match) return false
    }

    // 2. Plantation Zone Filter
    if (selectedZone.value !== 'all') {
      if (getStationZone(s) !== selectedZone.value) return false
    }

    // 3. Threat Level Filter
    if (selectedThreat.value !== 'all') {
      const level = s.threatLevel || s.confidenceTier
      if (selectedThreat.value === 'high' && level !== 'High') return false
      if (selectedThreat.value === 'medium' && level !== 'Medium') return false
      if (selectedThreat.value === 'low' && level !== 'Low' && level !== 'Safe') return false
    }

    // 4. Operating State Filter
    if (selectedOperatingState.value !== 'all') {
      if (selectedOperatingState.value === 'active' && s.isAcousticGated) return false
      if (selectedOperatingState.value === 'sleep' && !s.isAcousticGated) return false
      if (selectedOperatingState.value === 'master' && s.type !== 'master') return false
      if (selectedOperatingState.value === 'unconfigured' && s.isProfileComplete !== false) return false
    }

    // 5. Human Ground-Truth Audit Status Filter
    if (selectedAuditStatus.value !== 'all') {
      if (selectedAuditStatus.value === 'unverified' && s.humanVerificationStatus !== 'unverified') return false
      if (selectedAuditStatus.value === 'verified_clean' && s.humanVerificationStatus !== 'verified_clean') return false
      if (selectedAuditStatus.value === 'termites_present' && s.humanVerificationStatus !== 'termites_present') return false
    }

    return true
  })
})

const hasActiveFilters = computed(() => {
  return (
    searchQuery.value.trim() !== '' ||
    selectedZone.value !== 'all' ||
    selectedThreat.value !== 'all' ||
    selectedOperatingState.value !== 'all' ||
    selectedAuditStatus.value !== 'all'
  )
})

function resetFilters() {
  searchQuery.value = ''
  selectedZone.value = 'all'
  selectedThreat.value = 'all'
  selectedOperatingState.value = 'all'
  selectedAuditStatus.value = 'all'
  isSearchDropdownOpen.value = false
}

function selectSuggestion(station: Station) {
  searchQuery.value = station.code
  isSearchDropdownOpen.value = false
  store.selectStation(station.id)
}

function handleClickOutside(event: MouseEvent) {
  if (searchContainerRef.value && !searchContainerRef.value.contains(event.target as Node)) {
    isSearchDropdownOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside)
})

function openAddModal() {
  stationToEdit.value = null
  isModalOpen.value = true
}

function openEditModal(st: Station) {
  stationToEdit.value = st
  isModalOpen.value = true
}

function handleOpenVerify(st: Station) {
  stationForVerify.value = st.id
  isVerifyModalOpen.value = true
}

async function handleSimulateNewDiscovery() {
  const nextNum = store.stations.length
  const nextId = `ST-0${nextNum}`
  await store.simulateNewStationDiscovery(nextId)
}

async function handleSaveStation(data: any) {
  if (stationToEdit.value) {
    await store.updateStationDetails(stationToEdit.value.id, data)
  } else {
    await store.addNewStation(data)
  }
  isModalOpen.value = false
  stationToEdit.value = null
}

function getStationThreatBadge(station: Station) {
  if (station.isAcousticGated) {
    return {
      bg: 'bg-slate-800 text-slate-300 border-slate-700',
      dot: 'bg-slate-500',
      label: 'Safe (Gated)'
    }
  }
  const level = station.threatLevel || station.confidenceTier
  switch (level) {
    case 'High':
      return {
        bg: 'bg-rose-500/20 text-rose-300 border-rose-500/40',
        dot: 'bg-rose-500 animate-ping',
        label: 'High Threat'
      }
    case 'Medium':
      return {
        bg: 'bg-amber-500/20 text-amber-300 border-amber-500/40',
        dot: 'bg-amber-500',
        label: 'Medium Risk'
      }
    case 'Low':
      return {
        bg: 'bg-blue-500/20 text-blue-300 border-blue-500/40',
        dot: 'bg-blue-500',
        label: 'Low Risk'
      }
    default:
      return {
        bg: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40',
        dot: 'bg-emerald-500',
        label: 'Safe Baseline'
      }
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- View Header -->
    <div class="flex flex-wrap items-center justify-between gap-4 bg-slate-900/80 p-4 rounded-2xl border border-slate-800 shadow-lg">
      <div>
        <h2 class="text-lg font-black text-white flex items-center gap-2">
          <MapPin :size="20" class="text-emerald-400" />
          <span>Plantation Field Layout &amp; In-Ground Bait Stations</span>
        </h2>
        <p class="text-xs text-slate-400">Ultra-Low-Power Gated Architecture &bull; 15m Sampling when Moisture &ge; 65% &bull; 24h Daily Keepalive</p>
      </div>

      <div class="flex items-center gap-3">
        <button
          @click="handleSimulateNewDiscovery"
          class="bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 px-3.5 py-2 rounded-xl text-xs font-semibold shadow-lg flex items-center gap-1.5 transition"
          title="Simulate a newly deployed IoT bait station transmitting its initial LoRa packet"
        >
          <Radio :size="14" class="animate-pulse" />
          <span>Simulate New Go-Live</span>
        </button>

        <button
          @click="openAddModal"
          class="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-lg flex items-center gap-1.5 transition"
        >
          <Plus :size="16" />
          <span>Add New Station</span>
        </button>
      </div>
    </div>

    <!-- Dedicated GIS Station Search & Multi-Parameter Filter Bar -->
    <div class="bg-slate-900/90 p-4 rounded-2xl border border-slate-800 shadow-xl space-y-3">
      <div class="flex flex-wrap items-center justify-between gap-3 pb-2.5 border-b border-slate-800/80">
        <div class="flex items-center gap-2">
          <Filter :size="16" class="text-emerald-400" />
          <h3 class="text-xs font-bold text-white uppercase tracking-wider">GIS Station Filter &amp; Search</h3>
          <span class="text-xs text-slate-500 font-normal hidden sm:inline">&bull; Real-time map marker and table parameter filtering</span>
        </div>

        <div class="flex items-center gap-2.5">
          <span class="text-xs font-mono text-slate-400 bg-slate-950 px-2.5 py-1 rounded-lg border border-slate-800">
            Showing <strong class="text-emerald-400">{{ filteredStations.length }}</strong> of {{ store.stations.length }} stations
          </span>
          <button
            v-if="hasActiveFilters"
            @click="resetFilters"
            class="px-2.5 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/30 text-xs font-semibold flex items-center gap-1.5 transition shadow"
            title="Reset all active filters"
          >
            <RotateCcw :size="12" />
            <span>Reset Filters</span>
          </button>
        </div>
      </div>

      <!-- Controls Row: 5 Filter Parameters -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <!-- 1. Station Node Dynamic Autocomplete Search -->
        <div class="relative" ref="searchContainerRef">
          <label class="block text-[10px] font-mono uppercase text-slate-400 mb-1 font-semibold">Station Search</label>
          <div class="relative">
            <Search :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              v-model="searchQuery"
              @focus="isSearchDropdownOpen = true"
              @input="isSearchDropdownOpen = true"
              type="text"
              placeholder="Enter station (e.g. ST-02, Hub)..."
              class="w-full bg-slate-950 border border-slate-700 hover:border-slate-600 focus:border-emerald-500 rounded-xl pl-8 pr-8 py-2 text-xs font-semibold text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition shadow-inner"
            />
            <button
              v-if="searchQuery"
              @click="searchQuery = ''; isSearchDropdownOpen = false"
              class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white p-0.5 rounded-full"
              title="Clear search"
            >
              <X :size="13" />
            </button>
          </div>

          <!-- Dynamic Matching Dropdown Menu -->
          <div
            v-if="isSearchDropdownOpen && searchSuggestions.length > 0"
            class="absolute left-0 right-0 top-full mt-1.5 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl z-50 overflow-hidden divide-y divide-slate-800 max-h-64 overflow-y-auto"
          >
            <div class="px-3 py-1.5 bg-slate-950 text-[10px] font-mono text-slate-400 flex items-center justify-between">
              <span>Matching Nodes ({{ searchSuggestions.length }})</span>
              <span class="text-slate-500 text-[9px]">Click to filter &amp; select</span>
            </div>
            <button
              v-for="st in searchSuggestions"
              :key="st.id"
              @click="selectSuggestion(st)"
              class="w-full text-left px-3.5 py-2.5 hover:bg-slate-800/80 transition flex items-center justify-between gap-2.5 group"
            >
              <div class="flex items-center gap-2 min-w-0">
                <span
                  class="w-2 h-2 rounded-full shrink-0"
                  :class="!st.isAcousticGated ? 'bg-amber-400 animate-ping' : 'bg-indigo-400'"
                ></span>
                <div class="truncate">
                  <div class="flex items-center gap-1.5">
                    <span class="font-bold font-mono text-xs text-white group-hover:text-emerald-400 transition">{{ st.code }}</span>
                    <span v-if="st.type === 'master'" class="px-1 py-0.2 rounded bg-amber-950 text-amber-300 border border-amber-800 text-[9px] font-bold">HUB</span>
                    <span v-else-if="st.isProfileComplete === false" class="px-1 py-0.2 rounded bg-indigo-950 text-indigo-300 border border-indigo-700/60 text-[9px] font-bold">NEW</span>
                  </div>
                  <div class="text-[10px] text-slate-400 truncate">{{ st.name }} &bull; {{ st.treeTag || 'No Tree Tag' }}</div>
                </div>
              </div>
              <span
                class="text-[9px] px-1.5 py-0.5 rounded font-bold border shrink-0"
                :class="st.threatLevel === 'High' ? 'bg-rose-500/20 text-rose-300 border-rose-500/40' : st.threatLevel === 'Medium' ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' : 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'"
              >
                {{ st.threatLevel || 'Safe' }}
              </span>
            </button>
          </div>
        </div>

        <!-- 2. Plantation Zone Dropdown Selector -->
        <div>
          <label class="block text-[10px] font-mono uppercase text-slate-400 mb-1 font-semibold">Plantation Zone</label>
          <div class="relative">
            <select
              v-model="selectedZone"
              class="w-full bg-slate-950 border border-slate-700 hover:border-slate-600 focus:border-emerald-500 rounded-xl pl-3 pr-8 py-2 text-xs font-semibold text-white focus:outline-none focus:ring-1 focus:ring-emerald-500 cursor-pointer appearance-none shadow-inner"
            >
              <option value="all">All Plantation Zones ({{ store.stations.length }})</option>
              <option v-for="zone in availableZones" :key="zone" :value="zone">
                {{ zone }} ({{ getZoneStationCount(zone) }})
              </option>
            </select>
            <ChevronDown :size="13" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          </div>
        </div>

        <!-- 3. Infestation Threat Level Dropdown -->
        <div>
          <label class="block text-[10px] font-mono uppercase text-slate-400 mb-1 font-semibold">Infestation Threat</label>
          <div class="relative">
            <select
              v-model="selectedThreat"
              class="w-full bg-slate-950 border border-slate-700 hover:border-slate-600 focus:border-emerald-500 rounded-xl pl-3 pr-8 py-2 text-xs font-semibold text-white focus:outline-none focus:ring-1 focus:ring-emerald-500 cursor-pointer appearance-none shadow-inner"
            >
              <option value="all">All Threat Levels</option>
              <option value="high">High Threat (ITI &gt; 70)</option>
              <option value="medium">Medium Risk (ITI 40-70)</option>
              <option value="low">Safe / Low Baseline</option>
            </select>
            <ChevronDown :size="13" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          </div>
        </div>

        <!-- 4. Operating / Sensing State Dropdown -->
        <div>
          <label class="block text-[10px] font-mono uppercase text-slate-400 mb-1 font-semibold">Operating State</label>
          <div class="relative">
            <select
              v-model="selectedOperatingState"
              class="w-full bg-slate-950 border border-slate-700 hover:border-slate-600 focus:border-emerald-500 rounded-xl pl-3 pr-8 py-2 text-xs font-semibold text-white focus:outline-none focus:ring-1 focus:ring-emerald-500 cursor-pointer appearance-none shadow-inner"
            >
              <option value="all">All Operating Modes</option>
              <option value="active">15m Active (Moisture &ge; 65%)</option>
              <option value="sleep">Daily Keepalive (Sleep)</option>
              <option value="master">Master Hub Gateway</option>
              <option value="unconfigured">Awaiting Setup (New Go-Live)</option>
            </select>
            <ChevronDown :size="13" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          </div>
        </div>

        <!-- 5. Human Ground-Truth Audit Status Dropdown -->
        <div>
          <label class="block text-[10px] font-mono uppercase text-slate-400 mb-1 font-semibold">Human Ground Truth</label>
          <div class="relative">
            <select
              v-model="selectedAuditStatus"
              class="w-full bg-slate-950 border border-slate-700 hover:border-slate-600 focus:border-emerald-500 rounded-xl pl-3 pr-8 py-2 text-xs font-semibold text-white focus:outline-none focus:ring-1 focus:ring-emerald-500 cursor-pointer appearance-none shadow-inner"
            >
              <option value="all">All Audit Statuses</option>
              <option value="unverified">Needs Field Audit (Unverified)</option>
              <option value="verified_clean">Verified Clean</option>
              <option value="termites_present">Termites Confirmed</option>
            </select>
            <ChevronDown :size="13" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          </div>
        </div>
      </div>
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
            @click="openEditModal(unconfiguredSt)"
            class="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-2 transition shadow-lg shadow-indigo-950/50"
          >
            <Radio :size="14" />
            <span>Key In Station Profile</span>
            <ArrowRight :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- Plantation Map -->
    <PlantationMap
      height-class="h-[500px]"
      :stations="filteredStations"
      @open-add-modal="openAddModal"
      @configure-station="openEditModal"
      @select-station="st => store.selectStation(st.id)"
    />

    <!-- Stations Detailed Table -->
    <div class="bg-slate-900/90 rounded-2xl border border-slate-800 p-5 shadow-xl">
      <div class="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-slate-800">
        <div>
          <h3 class="font-bold text-white text-sm flex items-center gap-2">
            <span>Plantation Station Hardware Node &amp; Telemetry Parameters</span>
            <span class="text-xs font-mono font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
              stations table
            </span>
          </h3>
          <p class="text-xs text-slate-400 mt-0.5">
            Real-time multi-sensor telemetry, rolling Infestation Threat Index (ITI), and human ground-truth verification status (Scroll horizontally &rarr;)
          </p>
        </div>

        <div class="text-xs text-slate-400 flex items-center gap-3">
          <span class="flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full bg-amber-400 animate-ping"></span>
            <span>Active Sampling</span>
          </span>
          <span class="flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full bg-indigo-400"></span>
            <span>Gated Asleep</span>
          </span>
        </div>
      </div>

      <!-- Horizontal Scrollable Container -->
      <div class="overflow-x-auto rounded-xl border border-slate-800/80">
        <table class="w-full min-w-[1650px] text-left text-xs text-slate-300">
          <thead class="bg-slate-950 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800 font-mono">
            <tr>
              <th class="py-3 px-3.5 w-[180px]">Station Node</th>
              <th class="py-3 px-3.5 w-[170px]">Tree Tag &amp; Placement</th>
              <th class="py-3 px-3.5 w-[200px]">Threat Level &amp; ITI Index</th>
              <th class="py-3 px-3.5 w-[190px]">Human Ground Truth</th>
              <th class="py-3 px-3.5 w-[140px]">Soil Moisture</th>
              <th class="py-3 px-3.5 w-[150px]">Acoustic Sensor</th>
              <th class="py-3 px-3.5 w-[150px]">Cadence &amp; Uplink</th>
              <th class="py-3 px-3.5 w-[160px]">Microclimate &amp; RF</th>
              <th class="py-3 px-3.5 w-[130px]">Power &amp; Battery</th>
              <th class="py-3 px-3.5 w-[180px] text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-800/60 font-sans">
            <!-- Empty state when no stations match filter -->
            <tr v-if="filteredStations.length === 0">
              <td colspan="10" class="py-14 text-center text-slate-400">
                <div class="flex flex-col items-center justify-center gap-2.5">
                  <div class="w-10 h-10 rounded-xl bg-slate-800/60 text-slate-400 flex items-center justify-center border border-slate-700">
                    <Filter :size="20" />
                  </div>
                  <span class="text-sm font-semibold text-slate-200">No stations match the selected filter parameters</span>
                  <span class="text-xs text-slate-400 max-w-md">Try clearing or loosening your station search query, plantation zone, or threat level filters.</span>
                  <button
                    @click="resetFilters"
                    class="mt-2 px-3.5 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-semibold flex items-center gap-1.5 transition shadow"
                  >
                    <RotateCcw :size="13" />
                    <span>Reset All Filters</span>
                  </button>
                </div>
              </td>
            </tr>

            <tr
              v-for="st in filteredStations"
              :key="st.id"
              class="hover:bg-slate-800/40 transition"
              :class="store.selectedStationId === st.id ? 'bg-emerald-500/5' : ''"
            >
              <!-- 1. Station Node -->
              <td class="py-3.5 px-3.5 font-semibold text-white">
                <div class="flex items-center gap-2">
                  <span
                    class="w-2.5 h-2.5 rounded-full shrink-0"
                    :class="!st.isAcousticGated ? 'bg-amber-400 animate-ping' : 'bg-indigo-400'"
                  ></span>
                  <span class="font-bold font-mono text-sm">{{ st.code }}</span>
                  <span v-if="st.type === 'master'" class="px-1.5 py-0.2 rounded bg-amber-950 text-amber-300 border border-amber-800 text-[9px] font-bold">
                    HUB
                  </span>
                  <span v-else-if="st.isProfileComplete === false" class="px-1.5 py-0.2 rounded bg-indigo-950 text-indigo-300 border border-indigo-700/60 text-[9px] font-bold uppercase">
                    New Go-Live
                  </span>
                </div>
                <div class="text-[11px] text-slate-400 font-normal mt-0.5 truncate max-w-[170px]" :title="st.name">{{ st.name }}</div>
                <div class="text-[10px] text-slate-500 font-mono">{{ st.type === 'master' ? 'Master Hub (LoRaWAN + 5G)' : 'SX1262 LoRa Relay Node' }}</div>
              </td>

              <!-- 2. Tree Tag & Placement -->
              <td class="py-3.5 px-3.5">
                <template v-if="st.isProfileComplete === false">
                  <span class="text-indigo-400 text-xs font-semibold italic flex items-center gap-1">
                    <Radio :size="11" class="animate-pulse" />
                    <span>Profile Incomplete</span>
                  </span>
                  <div class="text-[10px] text-slate-500 font-sans font-normal">{{ st.zone }}</div>
                </template>
                <template v-else>
                  <span class="text-emerald-400 font-mono font-bold">{{ st.treeTag }}</span>
                  <div class="text-[10px] text-slate-300 font-sans font-normal">{{ st.zone }}</div>
                </template>
                <div class="text-[10px] text-slate-500 font-mono flex items-center gap-1 mt-0.5">
                  <MapPin :size="10" class="text-slate-500 shrink-0" />
                  <span>{{ st.coordinates[0].toFixed(5) }}, {{ st.coordinates[1].toFixed(5) }}</span>
                </div>
              </td>

              <!-- 3. Threat Level & ITI Index -->
              <td class="py-3.5 px-3.5">
                <div class="flex items-center gap-1.5 mb-1">
                  <span
                    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold border"
                    :class="getStationThreatBadge(st).bg"
                  >
                    <span class="w-1.5 h-1.5 rounded-full" :class="getStationThreatBadge(st).dot"></span>
                    <span>{{ getStationThreatBadge(st).label }}</span>
                  </span>
                  <span class="font-mono font-bold text-xs" :class="(st.threatIndex ?? 0) >= 80 ? 'text-rose-400' : (st.threatIndex ?? 0) >= 55 ? 'text-amber-400' : (st.threatIndex ?? 0) >= 30 ? 'text-blue-400' : 'text-emerald-400'">
                    ITI: {{ st.threatIndex ?? 0 }}/100
                  </span>
                </div>
                <!-- ITI Progress Bar -->
                <div class="w-32 bg-slate-800 h-1.5 rounded-full overflow-hidden mb-1">
                  <div
                    class="h-full rounded-full transition-all duration-300"
                    :class="(st.threatIndex ?? 0) >= 80 ? 'bg-rose-500' : (st.threatIndex ?? 0) >= 55 ? 'bg-amber-500' : (st.threatIndex ?? 0) >= 30 ? 'bg-blue-500' : 'bg-emerald-500'"
                    :style="{ width: `${st.threatIndex ?? 0}%` }"
                  ></div>
                </div>
                <div class="text-[10px] text-slate-400 truncate max-w-[190px]" :title="st.windowSummary">
                  {{ st.windowSummary || 'Evaluating rolling persistence...' }}
                </div>
              </td>

              <!-- 4. Human Ground Truth / SOP Verification -->
              <td class="py-3.5 px-3.5">
                <span
                  class="inline-block px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border mb-1"
                  :class="[
                    (st.humanVerificationStatus === 'verified_infestation' || st.humanVerificationStatus === 'field_verified_infestation') ? 'bg-rose-950/60 text-rose-300 border-rose-500/50' : '',
                    st.humanVerificationStatus === 'treatment_start' ? 'bg-amber-950/60 text-amber-300 border-amber-500/50' : '',
                    st.humanVerificationStatus === 'monitor' ? 'bg-indigo-950/60 text-indigo-300 border-indigo-500/50' : '',
                    st.humanVerificationStatus === 'verified_clean' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-500/50' : '',
                    st.humanVerificationStatus === 'false_alarm' ? 'bg-purple-950/60 text-purple-300 border-purple-500/50' : '',
                    (!st.humanVerificationStatus || st.humanVerificationStatus === 'unverified') ? 'bg-slate-800/80 text-slate-400 border-slate-700' : ''
                  ]"
                >
                  <template v-if="st.humanVerificationStatus === 'verified_clean'">
                    ✓ {{ formatCleanTimeAgo(st.verifiedCleanAt) }}
                  </template>
                  <template v-else-if="st.humanVerificationStatus === 'field_verified_infestation' || st.humanVerificationStatus === 'verified_infestation'">
                    ● Infestation Verified
                  </template>
                  <template v-else-if="st.humanVerificationStatus === 'treatment_start'">
                    ● In Treatment (CSI)
                  </template>
                  <template v-else-if="st.humanVerificationStatus === 'monitor'">
                    ● Monitoring (40d)
                  </template>
                  <template v-else-if="st.humanVerificationStatus === 'false_alarm'">
                    ⚠ False Alarm Cleared
                  </template>
                  <template v-else>
                    Unverified
                  </template>
                </span>
                <div class="text-[10px] text-slate-500">
                  {{ st.verifiedBy ? `${st.verifiedBy}` : 'Inspection pending' }}
                </div>
              </td>

              <!-- 5. Soil Moisture -->
              <td class="py-3.5 px-3.5">
                <div class="flex items-center justify-between text-xs mb-1">
                  <span class="font-mono font-bold" :class="st.soilMoisture >= st.moistureThreshold ? 'text-amber-400' : 'text-cyan-300'">
                    {{ st.soilMoisture }}%
                  </span>
                  <span class="text-[9px] font-bold" :class="st.soilMoisture >= st.moistureThreshold ? 'text-amber-400' : 'text-slate-500'">
                    {{ st.soilMoisture >= st.moistureThreshold ? 'TRIGGERED' : 'Below Trg' }}
                  </span>
                </div>
                <div class="w-24 bg-slate-800 h-1.5 rounded-full overflow-hidden mb-1">
                  <div
                    class="h-full rounded-full transition-all"
                    :class="st.soilMoisture >= st.moistureThreshold ? 'bg-amber-500' : 'bg-cyan-500'"
                    :style="{ width: `${st.soilMoisture}%` }"
                  ></div>
                </div>
                <div class="text-[10px] text-slate-500">Trg: {{ st.moistureThreshold }}%</div>
              </td>

              <!-- 6. Acoustic Sensor Activity -->
              <td class="py-3.5 px-3.5 font-mono">
                <div v-if="!st.isAcousticGated">
                  <div class="flex items-center gap-1.5 mb-1">
                    <span class="font-bold text-amber-400 text-xs">{{ st.activityScore }}/100</span>
                    <span class="text-[9px] px-1 py-0.2 rounded bg-amber-950 text-amber-300 border border-amber-800 font-sans">
                      15m Window
                    </span>
                  </div>
                  <div class="w-24 bg-slate-800 h-1.5 rounded-full overflow-hidden mb-1">
                    <div class="h-full bg-amber-500 rounded-full" :style="{ width: `${st.activityScore}%` }"></div>
                  </div>
                  <div class="text-[10px] text-amber-400/80 font-sans font-medium">Sampling 15m</div>
                </div>
                <div v-else>
                  <span class="inline-block px-2 py-0.5 rounded bg-slate-800 text-indigo-300 font-mono text-[10px] font-semibold border border-slate-700 mb-0.5">
                    DEEP SLEEP
                  </span>
                  <div class="text-[10px] text-slate-500 font-sans italic">
                    0 µA Standby &bull; Gated
                  </div>
                </div>
              </td>

              <!-- 7. Cadence & Transmission -->
              <td class="py-3.5 px-3.5 text-[11px] font-mono">
                <div class="text-slate-200 font-semibold flex items-center gap-1">
                  <Clock :size="11" class="text-slate-400" />
                  <span>{{ st.isAcousticGated ? '24h Daily Ping' : '15m Sampling' }}</span>
                </div>
                <div class="text-[10px] text-slate-400 mt-0.5">{{ st.nextExpectedTransmission }}</div>
                <div class="text-[9px] text-slate-500 mt-0.5">Last: {{ st.lastSeen }}</div>
              </td>

              <!-- 8. Microclimate & RF Link -->
              <td class="py-3.5 px-3.5 text-[11px] font-mono">
                <div class="text-slate-200 flex items-center gap-1">
                  <Thermometer :size="12" class="text-orange-400 shrink-0" />
                  <span>{{ st.temperature }}°C &bull; {{ st.humidity }}% RH</span>
                </div>
                <div class="text-[10px] text-slate-400 mt-0.5">
                  RSSI: <strong class="text-slate-300">{{ st.rssi }}</strong> dBm &bull; SNR: {{ st.snr }} dB
                </div>
              </td>

              <!-- 9. Power & Battery Health -->
              <td class="py-3.5 px-3.5">
                <div class="flex items-center gap-1.5 font-bold font-mono text-slate-200">
                  <component :is="st.isSolarCharging ? BatteryCharging : Battery" :size="14" class="text-emerald-400" />
                  <span>{{ st.batteryLevel }}%</span>
                </div>
                <div class="text-[10px] text-slate-400 font-mono mt-0.5">{{ st.batteryVoltage }}V</div>
                <div class="text-[9px] text-slate-500 font-sans">{{ st.type === 'master' ? 'Solar + LiFePO4' : 'Li-SOCl2 Pack' }}</div>
              </td>

              <!-- 10. Actions & Field Operations -->
              <td class="py-3.5 px-3.5 text-right">
                <div class="flex items-center justify-end gap-1.5">
                  <button
                    v-if="st.isProfileComplete === false"
                    @click="openEditModal(st)"
                    class="px-2 py-1 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-[10px] font-bold flex items-center gap-1 transition shadow shadow-indigo-950/40"
                    title="Complete Station Profile Metadata"
                  >
                    <Radio :size="11" />
                    <span>Key In Info</span>
                  </button>

                  <button
                    @click="handleOpenVerify(st)"
                    class="px-2 py-1 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-[10px] font-semibold flex items-center gap-1 transition shadow"
                    title="Log Field Inspection / Ground Truth Audit"
                  >
                    <ClipboardCheck :size="12" />
                    <span>Audit</span>
                  </button>

                  <button
                    @click="openEditModal(st)"
                    class="p-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
                    title="Edit Hardware &amp; Station Parameters"
                  >
                    <Edit :size="13" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modals -->
    <StationModal
      :is-open="isModalOpen"
      :station-to-edit="stationToEdit"
      @close="isModalOpen = false; stationToEdit = null"
      @save="handleSaveStation"
    />

    <FieldVerificationModal
      :is-open="isVerifyModalOpen"
      :preselected-station-id="stationForVerify"
      @close="isVerifyModalOpen = false"
    />
  </div>
</template>
