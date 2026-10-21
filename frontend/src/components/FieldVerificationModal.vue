<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { usePlantationStore } from '@/stores/plantationStore'
import { formatCleanTimeAgo } from '@/mocks/mockData'
import type { Station, CaseStatus } from '@/types'
import {
  X,
  ClipboardCheck,
  AlertTriangle,
  CheckCircle2,
  ShieldAlert,
  Search,
  UserCheck,
  Calendar,
  Sparkles,
  FlaskConical,
  Clock,
  ArrowRight,
  Info,
  Check,
  Ban
} from '@lucide/vue'

const props = defineProps<{
  isOpen: boolean
  preselectedStationId?: string
  preselectedCaseId?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'verified', result: any): void
}>()

const store = usePlantationStore()

const selectedStationId = ref<string>('')
const reportFinding = ref<'termites_present' | 'clean_no_termites'>('termites_present')
const technician = ref('Ju Esen (Field Technician)')
const auditNotes = ref('')
const probableCause = ref('mudpack_outside_chamber')
const isSubmitting = ref(false)
const feedbackMessage = ref('')
const feedbackType = ref<'success' | 'error'>('success')
const clearedCaseSnapshot = ref<any>(null)

// Active Case SOP transition inputs
const toxicantBatch = ref('CHITIN-CSI-2026-BATCH-09')
const sopNotes = ref('')

// Case Termination inputs
const showTerminateForm = ref(false)
const terminationNotes = ref('')

watch(() => props.isOpen, (open) => {
  if (open) {
    feedbackMessage.value = ''
    clearedCaseSnapshot.value = null
    showTerminateForm.value = false
    terminationNotes.value = ''
    if (props.preselectedCaseId) {
      const c = store.cases.find(caseItem => caseItem.id === props.preselectedCaseId)
      if (c) {
        selectedStationId.value = c.stationId
      } else {
        selectedStationId.value = props.preselectedStationId || store.selectedStationId || store.stations[0]?.id || ''
      }
    } else {
      selectedStationId.value = props.preselectedStationId || store.selectedStationId || store.stations[0]?.id || ''
    }
    auditNotes.value = ''
    sopNotes.value = ''
    reportFinding.value = 'termites_present'
  }
})

watch(() => props.preselectedStationId, (newId) => {
  if (newId) {
    selectedStationId.value = newId
    clearedCaseSnapshot.value = null
    showTerminateForm.value = false
    terminationNotes.value = ''
  }
})

watch(() => selectedStationId.value, () => {
  clearedCaseSnapshot.value = null
  showTerminateForm.value = false
  terminationNotes.value = ''
})

const currentStation = computed(() => {
  return store.stations.find(s => s.id === selectedStationId.value) || null
})

// Specific historical case if requested
const targetHistoricalCase = computed(() => {
  if (props.preselectedCaseId) {
    const c = store.cases.find(caseItem => caseItem.id === props.preselectedCaseId)
    if (c && c.stationId === selectedStationId.value) {
      return c
    }
  }
  return null
})

// Check if selected station has an ongoing human verification & response SOP case
const activeCase = computed(() => {
  if (!selectedStationId.value) return null
  return store.getActiveCaseForStation(selectedStationId.value) || null
})

// Current case display (targeted historical case, active case, or snapshot during clearance/termination)
const currentCase = computed(() => {
  return targetHistoricalCase.value || activeCase.value || clearedCaseSnapshot.value || null
})

// Show 4-stage SOP workflow when an active case exists
const showCaseLifecycle = computed(() => {
  return !!currentCase.value
})

const currentStageIndex = computed(() => {
  if (!currentCase.value) return 0
  switch (currentCase.value.status) {
    case 'field_verified_infestation': return 0
    case 'treatment_start': return 1
    case 'monitor': return 2
    case 'cleared': return 3
    case 'terminated': return 4
    default: return 0
  }
})

// Calculate how many events in the past 7 days will be affected
const affectedEventsCount = computed(() => {
  if (!selectedStationId.value) return 0
  const sevenDaysAgo = Date.now() - 7 * 24 * 3600 * 1000
  return store.allEvents.filter(e =>
    e.stationId === selectedStationId.value &&
    new Date(e.timestamp).getTime() >= sevenDaysAgo
  ).length
})

// Submit initial Field Audit (generates a VerificationCase in the backend)
async function handleCreateCase() {
  if (!selectedStationId.value) return
  isSubmitting.value = true
  feedbackMessage.value = ''

  try {
    const res = await store.createVerificationCase({
      stationId: selectedStationId.value,
      finding: reportFinding.value,
      technician: technician.value,
      notes: auditNotes.value || (reportFinding.value === 'termites_present'
        ? 'Field patrol confirmed live subterranean termites on root flare.'
        : 'Routine patrol: Clean tree, zero mudpack, healthy bark.'),
      probableCause: reportFinding.value === 'termites_present' ? probableCause.value : undefined
    })

    feedbackType.value = 'success'
    const generatedCaseId = res?.caseRecord?.id || 'CASE'
    if (reportFinding.value === 'termites_present') {
      feedbackMessage.value = `Verification Case ${generatedCaseId} generated! ${res?.stats?.labeledCount || 0} events retroactively labeled. Treatment SOP initiated.`
    } else {
      feedbackMessage.value = `Inspection recorded as Clean! Station returned to normal operation. ${res?.stats?.labeledCount || 0} events labeled as True Negatives (TN).`
    }

    emit('verified', { stationId: selectedStationId.value, case: res?.caseRecord })
    setTimeout(() => {
      emit('close')
    }, 1400)
  } catch (err: any) {
    feedbackType.value = 'error'
    feedbackMessage.value = `Error: ${err.message || 'Operation failed'}`
  } finally {
    isSubmitting.value = false
  }
}

// Transition Active Case to next lifecycle stage
async function handleTransitionCase(nextStatus: CaseStatus) {
  if (!activeCase.value) return
  const targetCase = activeCase.value
  const targetCaseId = targetCase.id
  isSubmitting.value = true
  feedbackMessage.value = ''

  // Cache snapshot for Stage 4 clearance animation so UI doesn't abruptly collapse
  if (nextStatus === 'cleared') {
    clearedCaseSnapshot.value = {
      ...targetCase,
      status: 'cleared',
      clearedNotes: sopNotes.value || 'Subterranean colony completely eliminated. Station verified clean & returned to baseline.'
    }
  }

  try {
    const res = await store.transitionVerificationCase(targetCaseId, nextStatus, {
      technician: technician.value,
      notes: sopNotes.value || (
        nextStatus === 'treatment_start'
          ? 'Toxicant placed in bait chamber; worker consumption initiated.'
          : nextStatus === 'monitor'
            ? '40-day periodic visual inspection & acoustic surveillance active.'
            : 'Subterranean colony completely eliminated. Station verified clean & returned to baseline.'
      ),
      toxicantBatch: nextStatus === 'treatment_start' ? toxicantBatch.value : undefined
    })

    feedbackType.value = 'success'
    if (nextStatus === 'treatment_start') {
      feedbackMessage.value = `Case ${targetCaseId} advanced to Treatment Start! Toxicant batch ${toxicantBatch.value} logged.`
    } else if (nextStatus === 'monitor') {
      feedbackMessage.value = `Case ${targetCaseId} advanced to 40-Day Monitoring Phase!`
    } else if (nextStatus === 'cleared') {
      feedbackMessage.value = `Case ${targetCaseId} successfully Cleared! Station returned to normal operation (Verified Clean Today).`
    }

    emit('verified', { stationId: selectedStationId.value, case: res?.caseRecord })
    setTimeout(() => {
      emit('close')
    }, 1500)
  } catch (err: any) {
    feedbackType.value = 'error'
    feedbackMessage.value = `Error: ${err.message || 'Transition failed'}`
    clearedCaseSnapshot.value = null
  } finally {
    isSubmitting.value = false
  }
}

// Terminate Active Case Early
async function handleTerminateCase() {
  if (!activeCase.value) return
  const targetCase = activeCase.value
  const targetCaseId = targetCase.id
  isSubmitting.value = true
  feedbackMessage.value = ''

  clearedCaseSnapshot.value = {
    ...targetCase,
    status: 'terminated',
    terminatedNotes: terminationNotes.value || 'Case terminated early by field technician.'
  }

  try {
    const res = await store.terminateVerificationCase(targetCaseId, {
      technician: technician.value,
      notes: terminationNotes.value || 'Case terminated early by field technician.'
    })

    feedbackType.value = 'success'
    feedbackMessage.value = `Case ${targetCaseId} has been Terminated. Station returned to baseline operation.`

    emit('verified', { stationId: selectedStationId.value, case: res?.caseRecord })
    setTimeout(() => {
      emit('close')
    }, 1500)
  } catch (err: any) {
    feedbackType.value = 'error'
    feedbackMessage.value = `Error: ${err.message || 'Termination failed'}`
    clearedCaseSnapshot.value = null
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
    <div class="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
      <!-- Modal Header -->
      <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between shrink-0">
        <div class="flex items-center gap-2.5">
          <div
            class="w-8 h-8 rounded-lg flex items-center justify-center border"
            :class="showCaseLifecycle ? 'bg-amber-500/20 text-amber-400 border-amber-500/30' : 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'"
          >
            <component :is="showCaseLifecycle ? ShieldAlert : ClipboardCheck" :size="18" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-white text-base">
                {{ showCaseLifecycle ? 'Response Protocol & SOP Case Lifecycle' : 'Field Inspection & Physical Audit' }}
              </h3>
              <span v-if="currentCase && showCaseLifecycle" class="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-bold border border-slate-700">
                {{ currentCase.id }}
              </span>
            </div>
            <p class="text-xs text-slate-400">
              {{ showCaseLifecycle ? 'Standard Operating Procedure: Verify → Treat → Monitor → Clear' : 'Physical Field Inspection & Station Verification' }}
            </p>
          </div>
        </div>

        <button @click="emit('close')" class="text-slate-400 hover:text-white p-1 rounded-lg">
          <X :size="18" />
        </button>
      </div>

      <!-- Modal Body (Scrollable) -->
      <div class="p-6 space-y-4 text-xs overflow-y-auto">
        <!-- Station Selector -->
        <div>
          <label class="block text-slate-300 font-semibold mb-1">Target Station</label>
          <select
            v-model="selectedStationId"
            class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white outline-none focus:border-emerald-500 font-medium"
          >
            <option v-for="st in store.stations" :key="st.id" :value="st.id">
              {{ st.code }} - {{ st.name }} (ITI: {{ st.threatIndex }}/100, {{ st.threatLevel }})
            </option>
          </select>
        </div>

        <!-- Current Sensor Diagnostic Context -->
        <div v-if="currentStation" class="bg-slate-950 p-3 rounded-xl border border-slate-800/80 flex items-center justify-between">
          <div>
            <div class="text-slate-400 text-[11px]">Sensor Calculated Threat Index (Pure & Algorithmic):</div>
            <div class="text-white font-mono font-bold text-sm flex items-center gap-2">
              <span>{{ currentStation.threatIndex }}/100</span>
              <span class="text-xs px-2 py-0.2 rounded font-sans uppercase font-bold"
                :class="currentStation.threatLevel === 'High' ? 'bg-rose-500/20 text-rose-300' : currentStation.threatLevel === 'Medium' ? 'bg-amber-500/20 text-amber-300' : currentStation.threatLevel === 'Low' ? 'bg-blue-500/20 text-blue-300' : 'bg-emerald-500/20 text-emerald-300'">
                {{ currentStation.threatLevel }} Risk
              </span>
            </div>
            <div class="text-[10px] text-slate-500">{{ currentStation.windowSummary }}</div>
          </div>
          <div class="text-right">
            <span class="text-[10px] px-2.5 py-1 rounded-full font-bold uppercase tracking-wider border inline-block"
              :class="[
                (currentStation.humanVerificationStatus === 'field_verified_infestation' || currentStation.humanVerificationStatus === 'verified_infestation') ? 'bg-rose-950/60 text-rose-300 border-rose-500/50' : '',
                currentStation.humanVerificationStatus === 'treatment_start' ? 'bg-amber-950/60 text-amber-300 border-amber-500/50' : '',
                currentStation.humanVerificationStatus === 'monitor' ? 'bg-indigo-950/60 text-indigo-300 border-indigo-500/50' : '',
                currentStation.humanVerificationStatus === 'verified_clean' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-500/50' : '',
                currentStation.humanVerificationStatus === 'false_alarm' ? 'bg-purple-950/60 text-purple-300 border-purple-500/50' : '',
                currentStation.humanVerificationStatus === 'terminated' ? 'bg-slate-800 text-slate-300 border-slate-600' : '',
                currentStation.humanVerificationStatus === 'unverified' || !currentStation.humanVerificationStatus ? 'bg-slate-800 text-slate-400 border-slate-700' : ''
              ]"
            >
              <template v-if="currentStation.humanVerificationStatus === 'verified_clean'">
                ✓ {{ formatCleanTimeAgo(currentStation.verifiedCleanAt) }}
              </template>
              <template v-else-if="currentStation.humanVerificationStatus === 'field_verified_infestation' || currentStation.humanVerificationStatus === 'verified_infestation'">
                ● Infestation Verified
              </template>
              <template v-else-if="currentStation.humanVerificationStatus === 'treatment_start'">
                ● In Treatment
              </template>
              <template v-else-if="currentStation.humanVerificationStatus === 'monitor'">
                ● Monitoring (40d)
              </template>
              <template v-else-if="currentStation.humanVerificationStatus === 'false_alarm'">
                ⚠ False Alarm Cleared
              </template>
              <template v-else-if="currentStation.humanVerificationStatus === 'terminated'">
                ⊘ Terminated
              </template>
              <template v-else>
                Unverified (Pending Audit)
              </template>
            </span>
          </div>
        </div>

        <!-- ======================================================== -->
        <!-- VIEW A: ACTIVE CASE LIFECYCLE STEPPER & TRANSITIONS      -->
        <!-- ======================================================== -->
        <div v-if="showCaseLifecycle && currentCase" class="space-y-4">
          <!-- Active Case Context Banner -->
          <div class="bg-amber-950/30 border border-amber-800/40 rounded-xl p-3 text-amber-200">
            <div class="flex items-center justify-between mb-1">
              <span class="font-bold flex items-center gap-1.5 text-xs text-amber-300">
                <ShieldAlert :size="14" />
                Incident Case: {{ currentCase.id }}
              </span>
              <span class="text-[10px] text-amber-400 font-mono">{{ currentCase.createdAt }}</span>
            </div>
            <p class="text-[11px] text-slate-300">
              Opened by <strong>{{ currentCase.technician }}</strong>. Finding:
              <span class="text-rose-400 font-semibold uppercase">Live Termites Confirmed</span>.
            </p>
            <p class="text-[11px] text-slate-400 italic mt-0.5">"{{ currentCase.initialNotes }}"</p>
          </div>

          <!-- 4-Stage Stepper: Field Verified -> Treatment Start -> Monitor -> Clear -->
          <div class="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <div class="text-[11px] font-semibold text-slate-400 mb-2">Response Protocol Pipeline:</div>
            <div class="grid grid-cols-4 gap-2 relative">
              <!-- Step 1: Field Verified -->
              <div
                class="p-2 rounded-lg border text-center transition flex flex-col items-center justify-between"
                :class="currentStageIndex >= 0 ? 'bg-rose-950/40 border-rose-500/50 text-rose-300' : 'bg-slate-900 border-slate-800 text-slate-500'"
              >
                <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                  :class="currentStageIndex > 0 ? 'bg-rose-500 text-white' : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'">
                  <Check v-if="currentStageIndex > 0" :size="11" />
                  <span v-else>1</span>
                </div>
                <div class="font-bold text-[10px] leading-tight">1. Field Verified</div>
                <div class="text-[9px] text-slate-400 mt-0.5">Termites Found</div>
              </div>

              <!-- Step 2: Treatment Start -->
              <div
                class="p-2 rounded-lg border text-center transition flex flex-col items-center justify-between"
                :class="currentStageIndex >= 1 ? 'bg-amber-950/40 border-amber-500/50 text-amber-300' : 'bg-slate-900 border-slate-800 text-slate-500'"
              >
                <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                  :class="currentStageIndex > 1 ? 'bg-amber-500 text-slate-950' : currentStageIndex === 1 ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 ring-2 ring-amber-500/30' : 'bg-slate-800 text-slate-500'">
                  <Check v-if="currentStageIndex > 1" :size="11" />
                  <span v-else>2</span>
                </div>
                <div class="font-bold text-[10px] leading-tight">2. Treat Start</div>
                <div class="text-[9px] text-slate-400 mt-0.5">CSI Bait Matrix</div>
              </div>

              <!-- Step 3: Monitor -->
              <div
                class="p-2 rounded-lg border text-center transition flex flex-col items-center justify-between"
                :class="currentStageIndex >= 2 ? 'bg-indigo-950/40 border-indigo-500/50 text-indigo-300' : 'bg-slate-900 border-slate-800 text-slate-500'"
              >
                <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                  :class="currentStageIndex > 2 ? 'bg-indigo-500 text-white' : currentStageIndex === 2 ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 ring-2 ring-indigo-500/30' : 'bg-slate-800 text-slate-500'">
                  <Check v-if="currentStageIndex > 2" :size="11" />
                  <span v-else>3</span>
                </div>
                <div class="font-bold text-[10px] leading-tight">3. Monitor</div>
                <div class="text-[9px] text-slate-400 mt-0.5">~40d Cycle</div>
              </div>

              <!-- Step 4: Cleared -->
              <div
                class="p-2 rounded-lg border text-center transition flex flex-col items-center justify-between"
                :class="currentStageIndex >= 3 ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-300 ring-2 ring-emerald-500/30' : 'bg-slate-900 border-slate-800 text-slate-500'"
              >
                <div class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold mb-1"
                  :class="currentStageIndex >= 3 ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-500'">
                  <Check v-if="currentStageIndex >= 3" :size="11" />
                  <span v-else>4</span>
                </div>
                <div class="font-bold text-[10px] leading-tight">4. Clear</div>
                <div class="text-[9px] text-slate-400 mt-0.5">Normal Baseline</div>
              </div>
            </div>
          </div>

          <!-- Step Progression Form -->
          <!-- STAGE 1: field_verified_infestation -> Advance to treatment_start -->
          <div v-if="currentCase.status === 'field_verified_infestation'" class="space-y-3 bg-slate-950/60 p-4 rounded-xl border border-slate-800">
            <div class="flex items-center gap-2 text-amber-300 font-bold">
              <FlaskConical :size="16" />
              <span>Step 2 Action: Deploy Termite Toxicant & Start Treatment</span>
            </div>
            <p class="text-[11px] text-slate-400 leading-snug">
              Infestation was physically verified. Record toxicant formulation batch and initiate baiting to suppress the underground colony.
            </p>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-slate-300 font-semibold mb-1 text-[11px]">Toxicant Batch #</label>
                <input
                  v-model="toxicantBatch"
                  type="text"
                  placeholder="e.g. CHITIN-CSI-2026-BATCH-09"
                  class="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-white outline-none focus:border-amber-500 font-mono"
                />
              </div>
              <div>
                <label class="block text-slate-300 font-semibold mb-1 text-[11px]">Technician</label>
                <input
                  v-model="technician"
                  type="text"
                  class="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-white outline-none focus:border-amber-500"
                />
              </div>
            </div>

            <div>
              <label class="block text-slate-300 font-semibold mb-1 text-[11px]">Treatment Application Notes</label>
              <textarea
                v-model="sopNotes"
                rows="2"
                placeholder="e.g. Placed 150g hexaflumuron matrix into in-ground chamber. Gallery sealed with bait cap."
                class="w-full bg-slate-900 border border-slate-800 rounded-lg p-2.5 text-white outline-none focus:border-amber-500"
              ></textarea>
            </div>

            <button
              type="button"
              :disabled="isSubmitting"
              @click="handleTransitionCase('treatment_start')"
              class="w-full py-2.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold flex items-center justify-center gap-2 transition shadow-lg shadow-amber-950/40"
            >
              <FlaskConical :size="15" />
              <span>Deploy Toxicant & Advance to Treatment Phase</span>
            </button>
          </div>

          <!-- STAGE 2: treatment_start -> Advance to monitor -->
          <div v-else-if="currentCase.status === 'treatment_start'" class="space-y-3 bg-slate-950/60 p-4 rounded-xl border border-slate-800">
            <div class="flex items-center gap-2 text-indigo-300 font-bold">
              <Clock :size="16" />
              <span>Step 3 Action: Initiate 40-Day Elimination Monitoring</span>
            </div>
            <div class="p-2.5 bg-slate-900 rounded-lg border border-slate-800 text-[11px] text-slate-300">
              <div class="flex justify-between">
                <span>Toxicant Batch: <strong class="text-amber-400 font-mono">{{ currentCase.toxicantBatch || 'CHITIN-CSI-2026-BATCH-09' }}</strong></span>
                <span class="text-slate-500">{{ currentCase.treatmentStartedAt }}</span>
              </div>
              <p class="text-slate-400 mt-1 italic">{{ currentCase.treatmentNotes }}</p>
            </div>

            <div>
              <label class="block text-slate-300 font-semibold mb-1 text-[11px]">Monitoring Inspection Notes</label>
              <textarea
                v-model="sopNotes"
                rows="2"
                placeholder="e.g. Worker feeding confirmed; acoustic frequency shifting downward as colony vigor declines."
                class="w-full bg-slate-900 border border-slate-800 rounded-lg p-2.5 text-white outline-none focus:border-indigo-500"
              ></textarea>
            </div>

            <button
              type="button"
              :disabled="isSubmitting"
              @click="handleTransitionCase('monitor')"
              class="w-full py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-bold flex items-center justify-center gap-2 transition shadow-lg shadow-indigo-950/40"
            >
              <Clock :size="15" />
              <span>Begin 40-Day Elimination Monitoring Phase</span>
            </button>
          </div>

          <!-- STAGE 3: monitor -> Advance to cleared -->
          <div v-else-if="currentCase.status === 'monitor'" class="space-y-3 bg-slate-950/60 p-4 rounded-xl border border-slate-800">
            <div class="flex items-center gap-2 text-emerald-300 font-bold">
              <CheckCircle2 :size="16" />
              <span>Step 4 Action: Confirm Eradication & Submit Clearance</span>
            </div>
            
            <div class="p-2.5 bg-emerald-950/30 border border-emerald-800/40 rounded-xl text-emerald-200 text-[11px] flex items-start gap-2">
              <Info :size="15" class="shrink-0 mt-0.5 text-emerald-400" />
              <div>
                <strong>Return to Normal Operation:</strong>
                Submitting clearance closes this case and sets the station to <strong>Verified Clean (Today)</strong>. The station returns to normal 24h baseline monitoring.
              </div>
            </div>

            <div>
              <label class="block text-slate-300 font-semibold mb-1 text-[11px]">Final Clearance Audit Notes</label>
              <textarea
                v-model="sopNotes"
                rows="2"
                placeholder="e.g. Physical inspection: bait matrix vacant, colony eradicated, zero live soldiers/workers. Sensor baseline normal."
                class="w-full bg-slate-900 border border-slate-800 rounded-lg p-2.5 text-white outline-none focus:border-emerald-500"
              ></textarea>
            </div>

            <button
              type="button"
              :disabled="isSubmitting"
              @click="handleTransitionCase('cleared')"
              class="w-full py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold flex items-center justify-center gap-2 transition shadow-lg shadow-emerald-950/40"
            >
              <CheckCircle2 :size="15" />
              <span>Submit Clearance & Return to Normal Operation</span>
            </button>
          </div>

          <!-- STAGE 4: Cleared confirmation state -->
          <div v-else-if="currentCase.status === 'cleared'" class="p-4 rounded-xl bg-emerald-950/40 border border-emerald-500/50 text-center space-y-2 animate-in fade-in">
            <div class="w-10 h-10 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto border border-emerald-500/40">
              <Check :size="20" />
            </div>
            <h4 class="font-bold text-white text-sm">Station Cleared &amp; Normal Baseline Restored</h4>
            <p class="text-xs text-emerald-300">Case resolved. Station marked as <strong>Verified Clean (Today)</strong>.</p>
            <p v-if="currentCase.clearedNotes" class="text-xs text-slate-300 italic bg-slate-900/80 p-2.5 rounded-lg border border-slate-800 text-left mt-2">
              "{{ currentCase.clearedNotes }}"
            </p>
            <div class="text-[10px] text-slate-400 font-mono pt-1">
              Cleared on: {{ currentCase.clearedAt || 'Recently' }}
            </div>
          </div>

          <!-- STAGE 5: Terminated confirmation state -->
          <div v-else-if="currentCase.status === 'terminated'" class="p-4 rounded-xl bg-slate-800/80 border border-slate-700 text-center space-y-2 animate-in fade-in">
            <div class="w-10 h-10 rounded-full bg-rose-500/20 text-rose-400 flex items-center justify-center mx-auto border border-rose-500/30">
              <Ban :size="20" />
            </div>
            <h4 class="font-bold text-white text-sm">Case Terminated Early</h4>
            <p class="text-xs text-slate-300">Response protocol cancelled. Station returned to baseline operation.</p>
            <p v-if="currentCase.terminatedNotes" class="text-xs text-rose-300 italic bg-rose-950/30 p-2.5 rounded-lg border border-rose-500/30 text-left mt-2">
              "{{ currentCase.terminatedNotes }}"
            </p>
            <div class="text-[10px] text-slate-400 font-mono pt-1">
              Terminated on: {{ currentCase.terminatedAt || 'Recorded' }}
            </div>
          </div>

          <!-- Terminate Case Section (Replacing old switch to re-audit) -->
          <div v-if="currentCase.status !== 'cleared' && currentCase.status !== 'terminated'" class="pt-3 border-t border-slate-800">
            <!-- Toggle Button -->
            <div v-if="!showTerminateForm" class="flex items-center justify-between">
              <span class="text-[11px] text-slate-500">Need to cancel or abort this incident response?</span>
              <button
                type="button"
                @click="showTerminateForm = true"
                class="text-xs text-rose-400 hover:text-rose-300 flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-rose-500/30 hover:bg-rose-950/30 transition font-medium"
              >
                <Ban :size="13" />
                <span>Terminate Case Early</span>
              </button>
            </div>

            <!-- Expanded Terminate Form -->
            <div v-else class="p-3.5 bg-rose-950/30 border border-rose-500/40 rounded-xl space-y-2.5 animate-in fade-in">
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold text-rose-300 flex items-center gap-1.5">
                  <Ban :size="14" />
                  <span>Early Case Termination</span>
                </span>
                <button
                  type="button"
                  @click="showTerminateForm = false"
                  class="text-slate-400 hover:text-white text-xs underline"
                >
                  Cancel
                </button>
              </div>

              <p class="text-[11px] text-slate-300 leading-snug">
                Terminating this case will cancel active response protocols and return the station to normal baseline monitoring.
              </p>

              <div>
                <label class="block text-[11px] font-semibold text-slate-300 mb-1">Reason for Termination</label>
                <textarea
                  v-model="terminationNotes"
                  rows="2"
                  placeholder="e.g. False lead: soil moisture spike was due to irrigation line leak; physical inspection confirmed no subterranean termite activity."
                  class="w-full bg-slate-900 border border-slate-800 rounded-lg p-2.5 text-white outline-none focus:border-rose-500 text-xs"
                ></textarea>
              </div>

              <div class="flex items-center justify-end gap-2 pt-1">
                <button
                  type="button"
                  @click="showTerminateForm = false"
                  class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
                >
                  Keep Case Active
                </button>
                <button
                  type="button"
                  :disabled="isSubmitting"
                  @click="handleTerminateCase"
                  class="px-3.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-lg shadow-rose-950/40"
                >
                  <Ban :size="13" />
                  <span>Confirm Termination</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- ======================================================== -->
        <!-- VIEW B: INITIAL AUDIT / GROUND TRUTH VERIFICATION FORM    -->
        <!-- ======================================================== -->
        <div v-else class="space-y-4">
          <!-- Finding Type Selector (2 Cards) -->
          <div>
            <div class="mb-1.5">
              <label class="block text-slate-300 font-semibold">Physical Field Finding</label>
            </div>
            
            <div class="grid grid-cols-2 gap-2">
              <!-- Active Termites Found Option -->
              <button
                type="button"
                @click="reportFinding = 'termites_present'"
                class="p-3 rounded-xl border text-left transition flex flex-col justify-between"
                :class="reportFinding === 'termites_present' ? 'bg-rose-950/40 border-rose-500/60 text-rose-300 ring-2 ring-rose-500/20' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'"
              >
                <div class="flex items-center gap-1.5 font-bold text-xs mb-1">
                  <AlertTriangle :size="14" class="text-rose-400 shrink-0" />
                  <span>Active Termites Found</span>
                </div>
                <p class="text-[10px] text-slate-400 leading-snug">
                  Colony infestation physically confirmed &rarr; Open <strong>Verification Case</strong> and initiate Treatment SOP.
                </p>
              </button>

              <!-- Routine Clean Option -->
              <button
                type="button"
                @click="reportFinding = 'clean_no_termites'"
                class="p-3 rounded-xl border text-left transition flex flex-col justify-between"
                :class="reportFinding === 'clean_no_termites' ? 'bg-emerald-950/40 border-emerald-500/60 text-emerald-300 ring-2 ring-emerald-500/20' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'"
              >
                <div class="flex items-center gap-1.5 font-bold text-xs mb-1">
                  <CheckCircle2 :size="14" class="text-emerald-400 shrink-0" />
                  <span>Station Clean / Safe</span>
                </div>
                <p class="text-[10px] text-slate-400 leading-snug">
                  No insect activity &rarr; Mark as <strong>Verified Clean</strong> (clears alarm if high risk).
                </p>
              </button>
            </div>
          </div>

          <!-- 7-Day Retroactive Window Banner -->
          <div class="p-2.5 rounded-xl bg-blue-950/30 border border-blue-800/40 text-blue-300 text-[11px] flex items-start gap-2">
            <Sparkles :size="14" class="shrink-0 mt-0.5 text-blue-400" />
            <div>
              <strong>7-Day Retroactive Verification Window:</strong>
              Submitting this audit will auto-label <strong>{{ affectedEventsCount }} telemetry events</strong> from the past 7 days to calculate live Model Confusion Matrix (TP, TN, FP, FN).
            </div>
          </div>

          <!-- Probable Cause (Only for Termites Found) -->
          <div v-if="reportFinding === 'termites_present'" class="space-y-1">
            <label class="block text-slate-300 font-semibold text-xs">Probable Cause for Missed Detection / Context</label>
            <select
              v-model="probableCause"
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white outline-none focus:border-rose-500"
            >
              <option value="mudpack_outside_chamber">Termite mudpack constructed outside internal sensor chamber</option>
              <option value="acoustic_threshold_too_high">Acoustic vibration threshold too high for low-density foraging</option>
              <option value="moisture_gating_delayed">Moisture gating delayed due to dry surface layer</option>
              <option value="bait_box_crack">Physical damage to outer bait station casing</option>
              <option value="other_hardware">Sensor wiring or piezo transducer degradation</option>
            </select>
          </div>

          <!-- Technician & Observations -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label class="block text-slate-300 font-semibold mb-1">Technician / Inspector</label>
              <input
                v-model="technician"
                type="text"
                class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label class="block text-slate-300 font-semibold mb-1">Inspection Date &amp; Time</label>
              <div class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-400 font-mono">
                Now (Physical Inspection)
              </div>
            </div>
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Field Observations / Evidence</label>
            <textarea
              v-model="auditNotes"
              rows="2"
              :placeholder="reportFinding === 'termites_present' ? 'e.g. Discovered active subterranean mudpack tube extending up southern flare; workers active.' : 'e.g. Inspected root flare; no mudpacking, healthy fibrous wood.'"
              class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-white outline-none focus:border-emerald-500"
            ></textarea>
          </div>

          <!-- Action Buttons for Initial Audit -->
          <div class="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              @click="emit('close')"
              class="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold transition"
            >
              Cancel
            </button>
            <button
              type="button"
              :disabled="isSubmitting"
              @click="handleCreateCase"
              class="px-4 py-2 rounded-lg font-bold flex items-center gap-1.5 transition shadow-lg"
              :class="reportFinding === 'termites_present' ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-950/50' : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-950/50'"
            >
              <UserCheck :size="15" />
              <span>{{ reportFinding === 'termites_present' ? 'Submit Termite Verification & Start SOP' : 'Log Routine Clean Pass' }}</span>
            </button>
          </div>
        </div>

        <!-- Feedback Alert Toast -->
        <div
          v-if="feedbackMessage"
          class="p-3 rounded-xl border text-xs font-semibold text-center transition-all animate-in fade-in"
          :class="feedbackType === 'success' ? 'bg-emerald-950/80 border-emerald-500/50 text-emerald-300' : 'bg-rose-950/80 border-rose-500/50 text-rose-300'"
        >
          {{ feedbackMessage }}
        </div>
      </div>
    </div>
  </div>
</template>
