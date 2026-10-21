<script setup lang="ts">
import { ref, computed } from 'vue'
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
  Compass
} from '@lucide/vue'

const store = usePlantationStore()
const router = useRouter()
const filterMode = ref<'all' | 'alerting' | 'unconfigured' | 'master'>('all')
const isModalOpen = ref(false)
const stationToEdit = ref<Station | null>(null)
const isVerifyModalOpen = ref(false)
const stationForVerify = ref<string>('')

const filteredStations = computed(() => {
  if (filterMode.value === 'alerting') {
    return store.stations.filter(s => !s.isAcousticGated)
  }
  if (filterMode.value === 'unconfigured') {
    return store.stations.filter(s => s.isProfileComplete === false)
  }
  if (filterMode.value === 'master') {
    return store.stations.filter(s => s.type === 'master')
  }
  return store.stations
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
    <div class="flex flex-wrap items-center justify-between gap-4 bg-slate-900/80 p-4 rounded-2xl border border-slate-800">
      <div>
        <h2 class="text-lg font-black text-white flex items-center gap-2">
          <MapPin :size="20" class="text-emerald-400" />
          <span>Plantation Field Layout & In-Ground Bait Stations</span>
        </h2>
        <p class="text-xs text-slate-400">Ultra-Low-Power Gated Architecture &bull; 15m Sampling when Moisture &ge; 65% &bull; 24h Daily Keepalive</p>
      </div>

      <div class="flex items-center gap-3">
        <!-- Filter Tabs -->
        <div class="bg-slate-950 p-1 rounded-xl border border-slate-800 flex items-center gap-1 text-xs">
          <button
            @click="filterMode = 'all'"
            class="px-3 py-1.5 rounded-lg font-medium transition"
            :class="filterMode === 'all' ? 'bg-emerald-600 text-white font-semibold' : 'text-slate-400 hover:text-white'"
          >
            All Stations ({{ store.stations.length }})
          </button>
          <button
            @click="filterMode = 'alerting'"
            class="px-3 py-1.5 rounded-lg font-medium transition"
            :class="filterMode === 'alerting' ? 'bg-amber-600 text-white font-semibold' : 'text-slate-400 hover:text-white'"
          >
            Acoustic Active ({{ store.triggeredStationsCount }})
          </button>
          <button
            v-if="store.unconfiguredStations.length > 0"
            @click="filterMode = 'unconfigured'"
            class="px-3 py-1.5 rounded-lg font-medium transition"
            :class="filterMode === 'unconfigured' ? 'bg-indigo-600 text-white font-semibold' : 'text-indigo-300 hover:text-white'"
          >
            Awaiting Setup ({{ store.unconfiguredStations.length }})
          </button>
          <button
            @click="filterMode = 'master'"
            class="px-3 py-1.5 rounded-lg font-medium transition"
            :class="filterMode === 'master' ? 'bg-slate-800 text-white font-semibold' : 'text-slate-400 hover:text-white'"
          >
            Master Hub
          </button>
        </div>

        <button
          @click="handleSimulateNewDiscovery"
          class="bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 px-3 py-2 rounded-xl text-xs font-semibold shadow-lg flex items-center gap-1.5 transition"
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
