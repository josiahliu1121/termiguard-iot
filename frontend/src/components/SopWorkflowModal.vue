<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AlertEvent, SopStage } from '@/types'
import {
  X,
  ShieldAlert,
  Search,
  FlaskConical,
  CheckCircle2,
  Calendar,
  AlertTriangle,
  FileCheck
} from '@lucide/vue'

const props = defineProps<{
  isOpen: boolean
  alert: AlertEvent | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'action', data: {
    alertId: string
    action: 'verify' | 'treat' | 'resolve' | 'mark_false_alarm'
    payload: { notes: string; technician?: string; toxicantBatch?: string }
  }): void
}>()

const currentAction = ref<'verify' | 'treat' | 'resolve' | 'mark_false_alarm'>('verify')
const notes = ref('')
const technician = ref('Ju Esen (Field Technician)')
const toxicantBatch = ref('CHITIN-CSI-2026-BATCH-09')

const stageOrder: SopStage[] = ['verify', 'treat', 'monitor', 'closed']

const activeStepIndex = computed(() => {
  if (!props.alert) return 0
  return stageOrder.indexOf(props.alert.sopStage)
})

function submitAction(actionType: 'verify' | 'treat' | 'resolve' | 'mark_false_alarm') {
  if (!props.alert) return
  emit('action', {
    alertId: props.alert.id,
    action: actionType,
    payload: {
      notes: notes.value,
      technician: technician.value,
      toxicantBatch: toxicantBatch.value
    }
  })
  notes.value = ''
  emit('close')
}
</script>

<template>
  <div v-if="isOpen && alert" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
    <div class="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden flex flex-col">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-lg bg-amber-500/20 text-amber-400 flex items-center justify-center border border-amber-500/30">
            <ShieldAlert :size="18" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-white text-base">Standard Operating Procedure (SOP)</h3>
              <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-bold">
                {{ alert.id }}
              </span>
            </div>
            <p class="text-xs text-slate-400">Response Pipeline: Verify &rarr; Treat &rarr; Monitor</p>
          </div>
        </div>

        <button @click="emit('close')" class="text-slate-400 hover:text-white p-1 rounded-lg">
          <X :size="18" />
        </button>
      </div>

      <!-- Alert Details Context Banner -->
      <div class="bg-slate-950/80 p-4 border-b border-slate-800/80 text-xs">
        <div class="flex items-center justify-between mb-2">
          <div class="font-semibold text-white flex items-center gap-2">
            <span>Station: <strong class="text-emerald-400">{{ alert.stationName }} ({{ alert.code }})</strong></span>
          </div>
          <span
            class="px-2.5 py-0.5 rounded-full font-bold text-[10px] uppercase tracking-wider"
            :class="alert.confidenceTier === 'High' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'"
          >
            {{ alert.confidenceTier }} Confidence Tier
          </span>
        </div>
        <p class="text-slate-300 text-[11px] leading-relaxed">{{ alert.summary }}</p>
      </div>

      <!-- SOP Stage Stepper -->
      <div class="px-6 py-4 border-b border-slate-800 bg-slate-900/50">
        <div class="flex items-center justify-between relative">
          <!-- Step 1: Verify -->
          <div class="flex flex-col items-center gap-1 z-10">
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs transition"
              :class="activeStepIndex >= 0 ? 'bg-amber-500 text-slate-950 shadow-lg shadow-amber-500/30' : 'bg-slate-800 text-slate-500'"
            >
              <Search :size="16" />
            </div>
            <span class="text-[10px] font-semibold text-slate-300">1. Verify</span>
          </div>

          <div class="flex-1 h-0.5 mx-2" :class="activeStepIndex >= 1 ? 'bg-amber-500' : 'bg-slate-800'"></div>

          <!-- Step 2: Treat -->
          <div class="flex flex-col items-center gap-1 z-10">
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs transition"
              :class="activeStepIndex >= 1 ? 'bg-rose-500 text-white shadow-lg shadow-rose-500/30' : 'bg-slate-800 text-slate-500'"
            >
              <FlaskConical :size="16" />
            </div>
            <span class="text-[10px] font-semibold text-slate-300">2. Treat</span>
          </div>

          <div class="flex-1 h-0.5 mx-2" :class="activeStepIndex >= 2 ? 'bg-rose-500' : 'bg-slate-800'"></div>

          <!-- Step 3: Monitor -->
          <div class="flex flex-col items-center gap-1 z-10">
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs transition"
              :class="activeStepIndex >= 2 ? 'bg-cyan-500 text-slate-950 shadow-lg shadow-cyan-500/30' : 'bg-slate-800 text-slate-500'"
            >
              <Calendar :size="16" />
            </div>
            <span class="text-[10px] font-semibold text-slate-300">3. Monitor (~40d)</span>
          </div>

          <div class="flex-1 h-0.5 mx-2" :class="activeStepIndex >= 3 ? 'bg-emerald-500' : 'bg-slate-800'"></div>

          <!-- Step 4: Closed -->
          <div class="flex flex-col items-center gap-1 z-10">
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs transition"
              :class="activeStepIndex >= 3 ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-500'"
            >
              <CheckCircle2 :size="16" />
            </div>
            <span class="text-[10px] font-semibold text-slate-300">4. Resolved</span>
          </div>
        </div>
      </div>

      <!-- Action Panel based on Current SOP Stage -->
      <div class="p-6 space-y-4 text-xs">
        <div>
          <label class="block text-slate-300 font-semibold mb-1">Technician / Inspector Name</label>
          <input
            v-model="technician"
            type="text"
            class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white outline-none focus:border-emerald-500"
          />
        </div>

        <!-- STAGE 1: VERIFY -->
        <div v-if="alert.sopStage === 'verify'" class="space-y-4">
          <div class="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl text-amber-300">
            <strong>Stage 1 Protocol:</strong> Inspect surrounding root flare and physical slotted bait housing. Check for subterranean termite mud tubes or soldier activity.
            <div class="mt-1 text-[11px] text-slate-300">
              ⚡ <em>Submitting verification automatically retro-labels the station's past 7 days of telemetry for model evaluation metrics.</em>
            </div>
          </div>

          <div v-if="currentAction === 'mark_false_alarm'" class="space-y-2 p-3 bg-rose-950/30 border border-rose-800/40 rounded-xl">
            <label class="block text-rose-300 font-semibold text-xs">Probable False Alarm Root Cause (FP)</label>
            <select
              v-model="notes"
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white outline-none focus:border-rose-500"
            >
              <option value="Rainstorm moisture surge & acoustic rain reverberation">Heavy Monsoon Rainstorm Surge (Moisture Spike)</option>
              <option value="Harmless black ant colony nesting in soil surface">Non-Termite Insects / Ant Colony Presence</option>
              <option value="Farm machinery & tractor vibration nearby">Farm Tractor / Heavy Machinery Ground Vibration</option>
              <option value="Temporary branch drop / physical disturbance">Animal / Foot Traffic Physical Impact</option>
              <option value="Moisture sensor baseline drift">Sensor Drift / Hardware Calibration Artifact</option>
            </select>
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Field Inspection Observations / Notes</label>
            <textarea
              v-model="notes"
              rows="3"
              placeholder="e.g. Confirmed active Coptotermes worker gallery penetrating into slotted chamber at 30cm depth."
              class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-white outline-none focus:border-amber-500"
            ></textarea>
          </div>

          <div class="flex items-center justify-between gap-3 pt-2">
            <button
              v-if="currentAction !== 'mark_false_alarm'"
              @click="currentAction = 'mark_false_alarm'"
              class="px-3.5 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-rose-300 border border-rose-900/40 font-semibold transition flex items-center gap-1.5"
            >
              <span>Flag False Positive (FP)</span>
            </button>
            <button
              v-else
              @click="submitAction('mark_false_alarm')"
              class="px-3.5 py-2 rounded-lg bg-rose-700 hover:bg-rose-600 text-white font-bold transition flex items-center gap-1.5"
            >
              <span>Confirm False Positive &rarr; Clear Alert</span>
            </button>

            <button
              @click="submitAction('verify')"
              class="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold flex items-center gap-1.5 transition shadow-lg shadow-emerald-900/50"
            >
              <FileCheck :size="15" />
              <span>Confirm Active Infestation (TP) &rarr; Treat</span>
            </button>
          </div>
        </div>

        <!-- STAGE 2: TREAT -->
        <div v-else-if="alert.sopStage === 'treat'" class="space-y-4">
          <div class="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-300">
            <strong>Stage 2 Protocol:</strong> Introduce cellulose-based attractant and Chitin Synthesis Inhibitor (CSI) toxicant into the underground bait station chamber.
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Toxicant Batch ID</label>
            <input
              v-model="toxicantBatch"
              type="text"
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white font-mono outline-none focus:border-rose-500"
            />
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Treatment Placement Log</label>
            <textarea
              v-model="notes"
              rows="2"
              placeholder="e.g. CSI Bait Matrix (120g) inserted into chamber. Sealed cap re-tightened."
              class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-white outline-none focus:border-rose-500"
            ></textarea>
          </div>

          <div class="flex justify-end pt-2">
            <button
              @click="submitAction('treat')"
              class="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-bold flex items-center gap-1.5 transition shadow-lg shadow-rose-950/50"
            >
              <FlaskConical :size="15" />
              <span>Record Bait Placement &rarr; Begin 40-Day Elimination Monitor</span>
            </button>
          </div>
        </div>

        <!-- STAGE 3: MONITOR -->
        <div v-else-if="alert.sopStage === 'monitor'" class="space-y-4">
          <div class="p-3 bg-cyan-500/10 border border-cyan-500/20 rounded-xl text-cyan-300">
            <strong>Stage 3 Protocol:</strong> Continuous autonomous monitoring under routine heartbeat schedule. Chitin inhibitor bait requires ~39.7 ± 2.5 days for complete trophallaxis colony collapse.
          </div>

          <div class="bg-slate-950 p-3 rounded-xl border border-slate-800 text-[11px] text-slate-300 space-y-1">
            <div>Verification Date: <strong class="text-slate-100">{{ alert.verificationDate || 'N/A' }}</strong></div>
            <div>Treatment Date: <strong class="text-slate-100">{{ alert.treatmentDate || 'N/A' }}</strong></div>
            <div>Toxicant Batch: <strong class="text-rose-400 font-mono">{{ alert.toxicantBatch || 'N/A' }}</strong></div>
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Final Resolution Notes</label>
            <textarea
              v-model="notes"
              rows="2"
              placeholder="e.g. Sub-surface vibration scores sustained below Safe baseline for 14 consecutive days. Infestation fully cleared."
              class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-white outline-none focus:border-emerald-500"
            ></textarea>
          </div>

          <div class="flex justify-end pt-2">
            <button
              @click="submitAction('resolve')"
              class="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold flex items-center gap-1.5 transition shadow-lg shadow-emerald-950/50"
            >
              <CheckCircle2 :size="15" />
              <span>Mark Termite Infestation Resolved</span>
            </button>
          </div>
        </div>

        <!-- CLOSED / RESOLVED -->
        <div v-else class="text-center py-4 text-emerald-400 space-y-2">
          <CheckCircle2 :size="32" class="mx-auto" />
          <h4 class="font-bold text-sm text-white">Alert Protocol Successfully Closed</h4>
          <p class="text-xs text-slate-400 max-w-sm mx-auto">
            This alert has been verified, treated, and confirmed resolved. Ground telemetry has returned to Safe baseline.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
