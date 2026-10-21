<script setup lang="ts">
import { ref, computed } from 'vue'
import { usePlantationStore } from '@/stores/plantationStore'
import FieldVerificationModal from '@/components/FieldVerificationModal.vue'
import { formatCleanTimeAgo } from '@/mocks/mockData'
import type { VerificationCase, AlertEvent, Station, CaseStatus } from '@/types'
import {
  ShieldAlert,
  Search,
  FlaskConical,
  Calendar,
  CheckCircle2,
  Clock,
  ArrowRight,
  Filter,
  ClipboardCheck,
  AlertTriangle,
  UserCheck,
  Sparkles,
  Droplets,
  Activity,
  Check,
  ChevronRight,
  Info,
  Bug,
  Tag,
  Plus,
  Ban
} from '@lucide/vue'

const store = usePlantationStore()

const searchQuery = ref('')
const selectedFilter = ref<'all' | 'active' | 'field_verified_infestation' | 'treatment_start' | 'monitor' | 'cleared' | 'terminated'>('all')
const isModalOpen = ref(false)
const stationForModal = ref<string>('')
const caseIdForModal = ref<string | undefined>(undefined)

// Open unified verification modal (identical to Dashboard Overview)
function openModalForStation(stationId: string, caseId?: string) {
  stationForModal.value = stationId
  caseIdForModal.value = caseId
  isModalOpen.value = true
}

function handleOpenNewAudit() {
  stationForModal.value = store.selectedStationId || store.stations[0]?.id || ''
  caseIdForModal.value = undefined
  isModalOpen.value = true
}

// Helper to look up station by ID
function getStation(stationId: string): Station | undefined {
  return store.stations.find(s => s.id === stationId)
}

// Find alert for a station if exists
function getAlertForStation(stationId: string): AlertEvent | undefined {
  return store.alerts.find(a => a.stationId === stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
}

// KPI Ribbon Counts
const stats = computed(() => {
  const activeCasesCount = store.cases.filter(c =>
    c.status !== 'cleared' && c.status !== 'routine_clean' && c.status !== 'false_alarm' && c.status !== 'terminated'
  ).length
  const fieldVerifiedCount = store.cases.filter(c => c.status === 'field_verified_infestation').length
  const inTreatmentCount = store.cases.filter(c => c.status === 'treatment_start').length
  const inMonitoringCount = store.cases.filter(c => c.status === 'monitor').length
  const clearedCount = store.cases.filter(c => c.status === 'cleared' || c.status === 'routine_clean').length
  const terminatedCount = store.cases.filter(c => c.status === 'terminated').length
  const activeAlertsCount = store.activeAlerts.length

  return {
    activeCasesCount,
    fieldVerifiedCount,
    inTreatmentCount,
    inMonitoringCount,
    clearedCount,
    terminatedCount,
    activeAlertsCount
  }
})

// Combined and filtered case list
const filteredCases = computed(() => {
  return store.cases.filter(c => {
    // 1. Filter by lifecycle status
    if (selectedFilter.value === 'active') {
      const isAct = c.status !== 'cleared' && c.status !== 'routine_clean' && c.status !== 'false_alarm' && c.status !== 'terminated'
      if (!isAct) return false
    } else if (selectedFilter.value === 'cleared') {
      const isClr = c.status === 'cleared' || c.status === 'routine_clean' || c.status === 'false_alarm'
      if (!isClr) return false
    } else if (selectedFilter.value === 'terminated') {
      if (c.status !== 'terminated') return false
    } else if (selectedFilter.value !== 'all') {
      if (c.status !== selectedFilter.value) {
        return false
      }
    }

    // 2. Filter by search query
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const st = getStation(c.stationId)
      const matchId = c.id.toLowerCase().includes(q)
      const matchCode = c.stationCode.toLowerCase().includes(q)
      const matchName = c.stationName.toLowerCase().includes(q)
      const matchTech = (c.technician || '').toLowerCase().includes(q)
      const matchNotes = (c.initialNotes || '').toLowerCase().includes(q)
      const matchBatch = (c.toxicantBatch || '').toLowerCase().includes(q)
      const matchTree = (st?.treeTag || '').toLowerCase().includes(q)
      const matchTermNotes = (c.terminatedNotes || '').toLowerCase().includes(q)
      return matchId || matchCode || matchName || matchTech || matchNotes || matchBatch || matchTree || matchTermNotes
    }

    return true
  })
})

// Unlinked sensor alerts (alerts that have no active SOP case yet)
const unlinkedAlerts = computed(() => {
  return store.activeAlerts.filter(a => !store.activeCases.some(c => c.stationId === a.stationId))
})

// Helper to determine stage step index for visual stepper
function getStepIndex(status: CaseStatus): number {
  switch (status) {
    case 'field_verified_infestation': return 1
    case 'treatment_start': return 2
    case 'monitor': return 3
    case 'cleared': return 4
    case 'terminated': return 0
    default: return 4
  }
}

// Stage configuration for visual badges
function getStatusBadgeConfig(status: CaseStatus) {
  switch (status) {
    case 'field_verified_infestation':
      return {
        bg: 'bg-rose-950/60 border-rose-500/50 text-rose-300',
        dot: 'bg-rose-500 animate-ping',
        label: 'Stage 1: Field Verified Infestation',
        btnText: 'Advance: Deploy Toxicant (Stage 2)',
        btnBg: 'bg-amber-500 hover:bg-amber-400 text-slate-950',
        btnIcon: FlaskConical
      }
    case 'treatment_start':
      return {
        bg: 'bg-amber-950/60 border-amber-500/50 text-amber-300',
        dot: 'bg-amber-500 animate-pulse',
        label: 'Stage 2: Treatment Active (CSI Matrix)',
        btnText: 'Advance: Begin 40d Monitor (Stage 3)',
        btnBg: 'bg-indigo-600 hover:bg-indigo-500 text-white',
        btnIcon: Clock
      }
    case 'monitor':
      return {
        bg: 'bg-indigo-950/60 border-indigo-500/50 text-indigo-300',
        dot: 'bg-indigo-400',
        label: 'Stage 3: 40-Day Elimination Surveillance',
        btnText: 'Confirm Eradication & Submit Clear (Stage 4)',
        btnBg: 'bg-emerald-600 hover:bg-emerald-500 text-white',
        btnIcon: CheckCircle2
      }
    case 'cleared':
      return {
        bg: 'bg-emerald-950/60 border-emerald-500/50 text-emerald-300',
        dot: 'bg-emerald-400',
        label: 'Stage 4: Cleared & Normal Operation',
        btnText: 'View Clearance Audit Record',
        btnBg: 'bg-slate-800 hover:bg-slate-700 text-slate-300',
        btnIcon: CheckCircle2
      }
    case 'terminated':
      return {
        bg: 'bg-rose-950/60 border-rose-500/50 text-rose-300',
        dot: 'bg-rose-500',
        label: 'Case Terminated Early',
        btnText: 'View Termination Record',
        btnBg: 'bg-slate-800 hover:bg-slate-700 text-slate-300',
        btnIcon: Ban
      }
    case 'false_alarm':
      return {
        bg: 'bg-purple-950/60 border-purple-500/50 text-purple-300',
        dot: 'bg-purple-400',
        label: 'False Alarm Cleared (True Negative)',
        btnText: 'View Audit Record',
        btnBg: 'bg-slate-800 hover:bg-slate-700 text-slate-300',
        btnIcon: CheckCircle2
      }
    case 'routine_clean':
      return {
        bg: 'bg-emerald-950/60 border-emerald-500/50 text-emerald-300',
        dot: 'bg-emerald-400',
        label: 'Routine Inspection Pass (Clean)',
        btnText: 'View Audit Record',
        btnBg: 'bg-slate-800 hover:bg-slate-700 text-slate-300',
        btnIcon: CheckCircle2
      }
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Top Header & Action Ribbon -->
    <div class="flex flex-wrap items-center justify-between gap-4 bg-slate-900/90 p-5 rounded-2xl border border-slate-800 shadow-xl">
      <div>
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center border border-amber-500/30">
            <ShieldAlert :size="20" />
          </div>
          <div>
            <h2 class="text-lg font-black text-white flex items-center gap-2">
              <span>Response SOP & Incident Cases Table</span>
              <span class="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-bold border border-slate-700">
                verification_cases
              </span>
            </h2>
            <p class="text-xs text-slate-400">
              End-to-end incident lifecycle tracking: 1. Field Verified &rarr; 2. Treatment Start &rarr; 3. 40d Monitor &rarr; 4. Clear &amp; Return to Baseline
            </p>
          </div>
        </div>
      </div>

      <!-- Log Field Audit Button (Opens unified modal) -->
      <button
        @click="handleOpenNewAudit"
        class="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs flex items-center gap-2 transition shadow-lg shadow-emerald-950/40"
      >
        <ClipboardCheck :size="16" />
        <span>Log Field Audit / Inspection</span>
      </button>
    </div>

    <!-- KPI Summary Ribbon: 4 Lifecycle Stat Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <!-- 1. Active SOP Cases -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <span class="text-slate-400 text-xs font-medium block mb-0.5">Active Incident Cases</span>
          <span class="text-2xl font-black font-mono text-amber-400">{{ stats.activeCasesCount }}</span>
          <span class="text-[10px] text-slate-500 block mt-0.5">In Verify / Treat / Monitor</span>
        </div>
        <div class="w-10 h-10 rounded-xl bg-amber-500/15 text-amber-400 flex items-center justify-center border border-amber-500/30">
          <ShieldAlert :size="18" />
        </div>
      </div>

      <!-- 2. In Treatment -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <span class="text-slate-400 text-xs font-medium block mb-0.5">Under Treatment</span>
          <span class="text-2xl font-black font-mono text-orange-400">{{ stats.inTreatmentCount }}</span>
          <span class="text-[10px] text-slate-500 block mt-0.5">CSI Hexaflumuron Matrix</span>
        </div>
        <div class="w-10 h-10 rounded-xl bg-orange-500/15 text-orange-400 flex items-center justify-center border border-orange-500/30">
          <FlaskConical :size="18" />
        </div>
      </div>

      <!-- 3. Elimination Monitoring -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <span class="text-slate-400 text-xs font-medium block mb-0.5">Active Surveillance</span>
          <span class="text-2xl font-black font-mono text-indigo-400">{{ stats.inMonitoringCount }}</span>
          <span class="text-[10px] text-slate-500 block mt-0.5">40-Day Periodic Colony Check</span>
        </div>
        <div class="w-10 h-10 rounded-xl bg-indigo-500/15 text-indigo-400 flex items-center justify-center border border-indigo-500/30">
          <Clock :size="18" />
        </div>
      </div>

      <!-- 4. Cleared & Verified Clean -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <span class="text-slate-400 text-xs font-medium block mb-0.5">Cleared &amp; Normal</span>
          <span class="text-2xl font-black font-mono text-emerald-400">{{ stats.clearedCount }}</span>
          <span class="text-[10px] text-slate-500 block mt-0.5">Colony Eradicated &bull; Baseline</span>
        </div>
        <div class="w-10 h-10 rounded-xl bg-emerald-500/15 text-emerald-400 flex items-center justify-center border border-emerald-500/30">
          <CheckCircle2 :size="18" />
        </div>
      </div>
    </div>

    <!-- Search & Filter Controls -->
    <div class="flex flex-wrap items-center justify-between gap-3 bg-slate-900/80 p-3 rounded-2xl border border-slate-800 text-xs">
      <!-- Search Input -->
      <div class="relative flex-1 min-w-[240px]">
        <Search :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search by Station Code, Tree Tag, Case ID, Technician, Batch #..."
          class="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-3 py-2 text-white placeholder-slate-500 outline-none focus:border-emerald-500 text-xs"
        />
      </div>

      <!-- Stage Filter Pills -->
      <div class="flex items-center gap-1 overflow-x-auto py-1">
        <button
          @click="selectedFilter = 'all'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0"
          :class="selectedFilter === 'all' ? 'bg-emerald-600 text-white font-semibold shadow' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          All ({{ store.cases.length }})
        </button>
        <button
          @click="selectedFilter = 'active'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0 flex items-center gap-1.5"
          :class="selectedFilter === 'active' ? 'bg-amber-600 text-white font-semibold shadow' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
          <span>Active SOP ({{ stats.activeCasesCount }})</span>
        </button>
        <button
          @click="selectedFilter = 'field_verified_infestation'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0"
          :class="selectedFilter === 'field_verified_infestation' ? 'bg-rose-900/80 border-rose-500 text-rose-200 font-semibold' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          1. Verified ({{ stats.fieldVerifiedCount }})
        </button>
        <button
          @click="selectedFilter = 'treatment_start'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0"
          :class="selectedFilter === 'treatment_start' ? 'bg-amber-900/80 border-amber-500 text-amber-200 font-semibold' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          2. Treat ({{ stats.inTreatmentCount }})
        </button>
        <button
          @click="selectedFilter = 'monitor'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0"
          :class="selectedFilter === 'monitor' ? 'bg-indigo-900/80 border-indigo-500 text-indigo-200 font-semibold' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          3. Monitor ({{ stats.inMonitoringCount }})
        </button>
        <button
          @click="selectedFilter = 'cleared'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0"
          :class="selectedFilter === 'cleared' ? 'bg-emerald-900/80 border-emerald-500 text-emerald-200 font-semibold' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          4. Cleared ({{ stats.clearedCount }})
        </button>
        <button
          @click="selectedFilter = 'terminated'"
          class="px-3 py-1.5 rounded-lg font-medium transition shrink-0 flex items-center gap-1"
          :class="selectedFilter === 'terminated' ? 'bg-rose-900/80 border-rose-500 text-rose-200 font-semibold' : 'text-slate-400 hover:text-white bg-slate-950 border border-slate-800'"
        >
          <Ban :size="12" />
          <span>Terminated ({{ stats.terminatedCount }})</span>
        </button>
      </div>
    </div>

    <!-- Unlinked Active Sensor Trigger Alerts (Awaiting Physical Verification) -->
    <div v-if="unlinkedAlerts.length > 0" class="space-y-3">
      <div class="text-xs font-bold text-amber-400 flex items-center gap-2">
        <AlertTriangle :size="15" />
        <span>Underground Sensor Alerts Awaiting Field Inspection ({{ unlinkedAlerts.length }})</span>
      </div>

      <div
        v-for="alert in unlinkedAlerts"
        :key="alert.id"
        class="bg-amber-950/20 border border-amber-500/40 rounded-2xl p-4 shadow-xl flex flex-wrap items-center justify-between gap-4"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-300 font-bold text-xs flex items-center justify-center border border-amber-500/30">
            {{ alert.code }}
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h4 class="font-bold text-white text-sm">{{ alert.stationName }}</h4>
              <span class="text-xs font-mono text-slate-400">({{ alert.id }})</span>
              <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-500/20 text-amber-300 border border-amber-500/40">
                {{ alert.confidenceTier }} Tier
              </span>
            </div>
            <p class="text-xs text-slate-300 mt-0.5">{{ alert.summary }}</p>
          </div>
        </div>

        <button
          @click="openModalForStation(alert.stationId)"
          class="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs flex items-center gap-1.5 transition shadow-lg shadow-amber-950/40"
        >
          <ClipboardCheck :size="15" />
          <span>Dispatch Field Audit &amp; Open Case</span>
          <ArrowRight :size="14" />
        </button>
      </div>
    </div>

    <!-- Main List: Verification Cases Cards -->
    <div class="space-y-4">
      <div
        v-for="caseItem in filteredCases"
        :key="caseItem.id"
        class="bg-slate-900/90 rounded-2xl border p-5 shadow-xl transition-all duration-200"
        :class="caseItem.status === 'terminated' ? 'border-rose-900/50 bg-slate-900/70 opacity-90' : (caseItem.status !== 'cleared' && caseItem.status !== 'routine_clean' && caseItem.status !== 'false_alarm' ? 'border-amber-500/40 bg-slate-900' : 'border-slate-800 bg-slate-900/70 opacity-90')"
      >
        <!-- Card Top Bar -->
        <div class="flex flex-wrap items-start justify-between gap-4 border-b border-slate-800/80 pb-4 mb-4">
          <!-- Station & Case Header -->
          <div class="flex items-center gap-3.5">
            <div
              class="w-12 h-12 rounded-xl flex items-center justify-center font-black text-sm border shadow-lg"
              :class="caseItem.status === 'terminated' ? 'bg-rose-500/10 text-rose-300 border-rose-500/30' : (caseItem.status !== 'cleared' && caseItem.status !== 'routine_clean' && caseItem.status !== 'false_alarm' ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' : 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40')"
            >
              {{ caseItem.stationCode.replace('ST-0', '').replace('ST-', '') }}
            </div>

            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="font-bold text-white text-base">{{ caseItem.stationName }}</h3>
                <span class="text-xs font-mono font-bold px-2 py-0.5 rounded bg-slate-950 text-amber-300 border border-slate-800">
                  {{ caseItem.id }}
                </span>
                <!-- Lifecycle Status Badge -->
                <span
                  class="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold border"
                  :class="getStatusBadgeConfig(caseItem.status).bg"
                >
                  <span class="w-1.5 h-1.5 rounded-full" :class="getStatusBadgeConfig(caseItem.status).dot"></span>
                  <span>{{ getStatusBadgeConfig(caseItem.status).label }}</span>
                </span>
              </div>

              <!-- Station Details & Inspector -->
              <p class="text-xs text-slate-400 mt-1 flex flex-wrap items-center gap-3">
                <span v-if="getStation(caseItem.stationId)" class="font-mono text-emerald-400">
                  Tree: {{ getStation(caseItem.stationId)?.treeTag }} &bull; {{ getStation(caseItem.stationId)?.zone }}
                </span>
                <span>&bull;</span>
                <span>Inspector: <strong class="text-slate-300">{{ caseItem.technician }}</strong></span>
                <span>&bull;</span>
                <span class="text-slate-500">Case Created: {{ caseItem.createdAt }}</span>
              </p>
            </div>
          </div>

          <!-- Action Button: Opens Unified FieldVerificationModal -->
          <button
            @click="openModalForStation(caseItem.stationId, caseItem.id)"
            class="px-4 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 transition shadow-lg shrink-0"
            :class="getStatusBadgeConfig(caseItem.status).btnBg"
          >
            <component :is="getStatusBadgeConfig(caseItem.status).btnIcon" :size="15" />
            <span>{{ getStatusBadgeConfig(caseItem.status).btnText }}</span>
            <ArrowRight :size="14" />
          </button>
        </div>

        <!-- 4-Stage Protocol Visual Stepper -->
        <div class="mb-4 bg-slate-950 p-3 rounded-xl border border-slate-800/80">
          <div class="flex items-center justify-between text-[11px] text-slate-400 font-semibold mb-2">
            <span>SOP Response Progress Pipeline:</span>
            <span v-if="caseItem.status === 'cleared' || caseItem.status === 'routine_clean'" class="text-emerald-400 font-mono font-bold flex items-center gap-1">
              <CheckCircle2 :size="12" /> Complete Eradication Verified
            </span>
            <span v-else-if="caseItem.status === 'terminated'" class="text-rose-400 font-mono font-bold flex items-center gap-1">
              <Ban :size="12" /> Case Terminated Early
            </span>
          </div>

          <div class="grid grid-cols-4 gap-2 text-center text-xs">
            <!-- Step 1: Field Verified -->
            <div
              class="p-2 rounded-lg border flex flex-col items-center justify-between"
              :class="(caseItem.status === 'terminated' || getStepIndex(caseItem.status) >= 1) ? 'bg-rose-950/40 border-rose-500/50 text-rose-300' : 'bg-slate-900 border-slate-800 text-slate-500'"
            >
              <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                :class="(caseItem.status === 'terminated' || getStepIndex(caseItem.status) > 1) ? 'bg-rose-500 text-white' : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'">
                <Check v-if="caseItem.status === 'terminated' || getStepIndex(caseItem.status) > 1" :size="11" />
                <span v-else>1</span>
              </div>
              <span class="font-bold text-[10px]">1. Field Verified</span>
              <span class="text-[9px] text-slate-400 mt-0.5 truncate">{{ caseItem.verifiedAt?.split(' ')[1] || 'Verified' }}</span>
            </div>

            <!-- Step 2: Treatment Start -->
            <div
              class="p-2 rounded-lg border flex flex-col items-center justify-between"
              :class="(caseItem.treatmentStartedAt || getStepIndex(caseItem.status) >= 2) ? 'bg-amber-950/40 border-amber-500/50 text-amber-300' : 'bg-slate-900 border-slate-800 text-slate-500'"
            >
              <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                :class="(caseItem.treatmentStartedAt && (caseItem.monitoredAt || getStepIndex(caseItem.status) > 2)) ? 'bg-amber-500 text-slate-950' : (caseItem.treatmentStartedAt || getStepIndex(caseItem.status) === 2) ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 ring-2 ring-amber-500/30' : 'bg-slate-800 text-slate-500'">
                <Check v-if="caseItem.treatmentStartedAt && (caseItem.monitoredAt || getStepIndex(caseItem.status) > 2)" :size="11" />
                <span v-else>2</span>
              </div>
              <span class="font-bold text-[10px]">2. Treat Start</span>
              <span class="text-[9px] text-slate-400 mt-0.5 truncate">{{ caseItem.toxicantBatch || 'Pending CSI' }}</span>
            </div>

            <!-- Step 3: Monitor -->
            <div
              class="p-2 rounded-lg border flex flex-col items-center justify-between"
              :class="(caseItem.monitoredAt || getStepIndex(caseItem.status) >= 3) ? 'bg-indigo-950/40 border-indigo-500/50 text-indigo-300' : 'bg-slate-900 border-slate-800 text-slate-500'"
            >
              <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                :class="(caseItem.monitoredAt && (caseItem.clearedAt || getStepIndex(caseItem.status) > 3)) ? 'bg-indigo-500 text-white' : (caseItem.monitoredAt || getStepIndex(caseItem.status) === 3) ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 ring-2 ring-indigo-500/30' : 'bg-slate-800 text-slate-500'">
                <Check v-if="caseItem.monitoredAt && (caseItem.clearedAt || getStepIndex(caseItem.status) > 3)" :size="11" />
                <span v-else>3</span>
              </div>
              <span class="font-bold text-[10px]">3. Monitor (40d)</span>
              <span class="text-[9px] text-slate-400 mt-0.5 truncate">{{ caseItem.monitoredAt ? 'Surveillance Active' : 'Pending Treat' }}</span>
            </div>

            <!-- Step 4: Cleared or Terminated -->
            <div
              class="p-2 rounded-lg border flex flex-col items-center justify-between"
              :class="caseItem.status === 'terminated' ? 'bg-rose-950/40 border-rose-500/50 text-rose-300' : (getStepIndex(caseItem.status) >= 4 ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-300' : 'bg-slate-900 border-slate-800 text-slate-500')"
            >
              <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                :class="caseItem.status === 'terminated' ? 'bg-rose-500 text-white' : (getStepIndex(caseItem.status) >= 4 ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-500')">
                <Ban v-if="caseItem.status === 'terminated'" :size="11" />
                <Check v-else-if="getStepIndex(caseItem.status) >= 4" :size="11" />
                <span v-else>4</span>
              </div>
              <span class="font-bold text-[10px]">{{ caseItem.status === 'terminated' ? 'Terminated' : '4. Cleared Baseline' }}</span>
              <span class="text-[9px] text-slate-400 mt-0.5 truncate">{{ caseItem.status === 'terminated' ? 'Closed Early' : (caseItem.clearedAt ? 'Clean Verified' : 'In Progress') }}</span>
            </div>
          </div>
        </div>

        <!-- Rich Case Details Grid (From Table 3) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs mb-3">
          <!-- Col 1: Field Audit Finding & Evidence -->
          <div class="bg-slate-950/80 p-3 rounded-xl border border-slate-800/80 flex flex-col justify-between">
            <div>
              <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-semibold mb-1">
                <ClipboardCheck :size="13" class="text-rose-400" />
                <span>1. Field Finding &amp; Evidence</span>
              </div>
              <div class="font-bold text-slate-200 mb-1 flex items-center gap-1.5">
                <span class="w-1.5 h-1.5 rounded-full" :class="caseItem.finding === 'termites_present' ? 'bg-rose-500' : 'bg-emerald-500'"></span>
                <span>{{ caseItem.finding === 'termites_present' ? 'Live Termites Present (Confirmed)' : 'Station Clean / Pass' }}</span>
              </div>
              <p class="text-[11px] text-slate-300 italic mb-2 leading-relaxed">
                "{{ caseItem.initialNotes }}"
              </p>
            </div>
            <div v-if="caseItem.probableCause" class="pt-2 border-t border-slate-800/60 text-[10px] text-slate-400">
              <span class="text-slate-500">Root Cause:</span> {{ caseItem.probableCause.replace(/_/g, ' ') }}
            </div>
          </div>

          <!-- Col 2: Toxicant Treatment Details -->
          <div class="bg-slate-950/80 p-3 rounded-xl border border-slate-800/80 flex flex-col justify-between">
            <div>
              <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-semibold mb-1">
                <FlaskConical :size="13" class="text-amber-400" />
                <span>2. Toxicant Deployment Record</span>
              </div>
              <div class="mb-1">
                <span class="text-slate-500 text-[10px]">Formulation Batch:</span>
                <span class="font-mono font-bold text-amber-300 ml-1.5">{{ caseItem.toxicantBatch || 'Pending formulation' }}</span>
              </div>
              <p v-if="caseItem.treatmentNotes" class="text-[11px] text-slate-300 italic mb-2 leading-relaxed">
                "{{ caseItem.treatmentNotes }}"
              </p>
              <p v-else class="text-[11px] text-slate-500 italic">
                Awaiting bait matrix deployment in Step 2.
              </p>
            </div>
            <div v-if="caseItem.treatmentStartedAt" class="pt-2 border-t border-slate-800/60 text-[10px] text-slate-400">
              <span class="text-slate-500">Applied:</span> {{ caseItem.treatmentStartedAt }}
            </div>
          </div>

          <!-- Col 3: Monitoring & Final Resolution Trail -->
          <div class="bg-slate-950/80 p-3 rounded-xl border border-slate-800/80 flex flex-col justify-between">
            <div>
              <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-semibold mb-1">
                <Clock :size="13" class="text-indigo-400" />
                <span>3. Monitoring &amp; Final Resolution</span>
              </div>
              <div v-if="caseItem.status === 'terminated'" class="mb-1 text-rose-400 font-bold flex items-center gap-1">
                <Ban :size="12" /> Case Terminated Early ({{ caseItem.terminatedAt?.split(' ')[0] || 'Recorded' }})
              </div>
              <div v-else-if="caseItem.clearedAt" class="mb-1 text-emerald-300 font-bold flex items-center gap-1">
                <Check :size="12" /> Eradication Confirmed ({{ caseItem.clearedAt.split(' ')[0] }})
              </div>
              <p v-if="caseItem.status === 'terminated' && caseItem.terminatedNotes" class="text-[11px] text-rose-300 italic mb-2 leading-relaxed">
                "{{ caseItem.terminatedNotes }}"
              </p>
              <p v-else-if="caseItem.clearedNotes" class="text-[11px] text-slate-300 italic mb-2 leading-relaxed">
                "{{ caseItem.clearedNotes }}"
              </p>
              <p v-else-if="caseItem.monitoringNotes" class="text-[11px] text-slate-300 italic mb-2 leading-relaxed">
                "{{ caseItem.monitoringNotes }}"
              </p>
              <p v-else class="text-[11px] text-slate-500 italic">
                Awaiting 40-day surveillance observation cycle.
              </p>
            </div>
            <div v-if="getStation(caseItem.stationId)" class="pt-2 border-t border-slate-800/60 flex items-center justify-between text-[10px]">
              <span class="text-slate-500">Station Status:</span>
              <span class="font-bold text-emerald-400">
                {{ getStation(caseItem.stationId)?.humanVerificationStatus === 'verified_clean' ? formatCleanTimeAgo(getStation(caseItem.stationId)?.verifiedCleanAt) : (getStation(caseItem.stationId)?.humanVerificationStatus?.replace(/_/g, ' ') || 'Unverified') }}
              </span>
            </div>
          </div>
        </div>

        <!-- Sensor Calculated Diagnostic Strip (Pure Mathematical Telemetry) -->
        <div v-if="getStation(caseItem.stationId)" class="bg-slate-950/60 px-3 py-2 rounded-xl border border-slate-800 flex flex-wrap items-center justify-between gap-3 text-[11px]">
          <div class="flex items-center gap-4 text-slate-400">
            <span>
              Soil Moisture: <strong class="text-cyan-300 font-mono">{{ getStation(caseItem.stationId)?.soilMoisture }}%</strong>
            </span>
            <span>
              Acoustic Vibration: <strong class="text-amber-400 font-mono">{{ getStation(caseItem.stationId)?.activityScore ?? '0 (Sleep)' }}/100</strong>
            </span>
            <span>
              Algorithmic ITI Score: <strong class="text-white font-mono">{{ getStation(caseItem.stationId)?.threatIndex }}/100</strong>
              <span class="ml-1 text-[10px] px-1.5 py-0.2 rounded font-sans font-bold uppercase"
                :class="getStation(caseItem.stationId)?.threatLevel === 'High' ? 'bg-rose-500/20 text-rose-300' : 'bg-emerald-500/20 text-emerald-300'">
                {{ getStation(caseItem.stationId)?.threatLevel }}
              </span>
            </span>
          </div>

          <div class="text-slate-500 text-[10px]">
            {{ getStation(caseItem.stationId)?.windowSummary }}
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="filteredCases.length === 0 && unlinkedAlerts.length === 0" class="text-center py-12 bg-slate-900/50 rounded-2xl border border-slate-800">
        <ShieldAlert :size="32" class="mx-auto text-slate-600 mb-2" />
        <h3 class="font-bold text-white text-sm mb-1">No Incident Cases Match Filter</h3>
        <p class="text-xs text-slate-400 mb-4">Try clearing your search query or selecting a different lifecycle stage filter.</p>
        <button
          @click="selectedFilter = 'all'; searchQuery = ''"
          class="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
        >
          Reset Filters
        </button>
      </div>
    </div>

    <!-- Unified SOP & Field Verification Modal (Identical to Dashboard Overview) -->
    <FieldVerificationModal
      :is-open="isModalOpen"
      :preselected-station-id="stationForModal"
      :preselected-case-id="caseIdForModal"
      @close="isModalOpen = false; caseIdForModal = undefined"
    />
  </div>
</template>
