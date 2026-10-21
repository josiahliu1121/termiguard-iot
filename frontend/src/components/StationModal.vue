<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Station, StationInfo } from '@/types'
import { X, Save, MapPin, Database, Trees, Radio, Info } from '@lucide/vue'

const props = defineProps<{
  isOpen: boolean
  stationToEdit?: Station | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', data: Partial<StationInfo>): void
}>()

const form = ref({
  code: '',
  name: '',
  description: '',
  treeTag: '',
  plantationZone: '',
  type: 'peripheral' as 'master' | 'peripheral',
  lat: 2.5896,
  lng: 102.0917,
  moistureThreshold: 65.0,
  samplingFrequencyMinutes: 15,
  heartbeatIntervalHours: 24,
  isSolarPowered: false
})

watch(() => props.stationToEdit, (val) => {
  if (val) {
    form.value = {
      code: val.id || val.code || '',
      name: val.name || `Station ${val.id}`,
      description: val.description || '',
      treeTag: val.treeTag || '',
      plantationZone: val.plantationZone || val.zone || '',
      type: val.type || 'peripheral',
      lat: val.coordinates ? val.coordinates[0] : 2.5898,
      lng: val.coordinates ? val.coordinates[1] : 102.0931,
      moistureThreshold: val.moistureThreshold || 65.0,
      samplingFrequencyMinutes: val.samplingFrequencyMinutes || 15,
      heartbeatIntervalHours: val.heartbeatIntervalHours || 24,
      isSolarPowered: val.type === 'master'
    }
  } else {
    form.value = {
      code: 'ST-05',
      name: 'South-East Expansion Station',
      description: 'Peripheral station monitoring southern Paulownia root zone.',
      treeTag: 'PW-RMB-0145',
      plantationZone: 'Zone D - South Expansion',
      type: 'peripheral',
      lat: 2.5891,
      lng: 102.0934,
      moistureThreshold: 65.0,
      samplingFrequencyMinutes: 15,
      heartbeatIntervalHours: 24,
      isSolarPowered: false
    }
  }
}, { immediate: true })

function handleSubmit() {
  emit('save', {
    id: form.value.code,
    code: form.value.code,
    name: form.value.name,
    description: form.value.description,
    treeTag: form.value.treeTag,
    plantationZone: form.value.plantationZone,
    type: form.value.type,
    coordinates: [Number(form.value.lat), Number(form.value.lng)] as [number, number],
    moistureThreshold: Number(form.value.moistureThreshold),
    samplingFrequencyMinutes: Number(form.value.samplingFrequencyMinutes),
    heartbeatIntervalHours: Number(form.value.heartbeatIntervalHours),
    isSolarPowered: form.value.type === 'master',
    isProfileComplete: true
  })
  emit('close')
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
    <div class="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden flex flex-col">
      <!-- Modal Header -->
      <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Database :size="18" class="text-emerald-400" />
          <div>
            <h3 class="font-bold text-white text-base">
              {{ stationToEdit?.isProfileComplete === false ? 'Complete New Station Profile' : stationToEdit ? 'Modify Station Information Table' : 'Add to Station Information Table' }}
            </h3>
            <p class="text-[11px] text-slate-400">Database Table: stations &bull; Unified station_id</p>
          </div>
        </div>
        <button @click="emit('close')" class="text-slate-400 hover:text-white p-1 rounded-lg">
          <X :size="18" />
        </button>
      </div>

      <!-- Modal Body -->
      <form @submit.prevent="handleSubmit" class="p-6 space-y-4 text-xs">
        <!-- New Station Live Auto-Discovery Alert -->
        <div v-if="stationToEdit?.isProfileComplete === false" class="p-3.5 bg-indigo-950/40 border border-indigo-500/40 rounded-xl flex items-start gap-3 text-xs text-indigo-300">
          <div class="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center shrink-0 border border-indigo-500/30">
            <Radio :size="16" class="animate-pulse" />
          </div>
          <div>
            <span class="font-bold text-white block mb-0.5">Auto-Registered from Initial Telemetry Stream</span>
            <span class="leading-relaxed text-[11px] text-slate-300">
              Station <strong class="text-white">{{ form.code }}</strong> transmitted its initial packet and joined the network. Telemetry &amp; Threat Index monitoring are already live. Key in the field assignment details to complete its profile.
            </span>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-slate-300 font-semibold mb-1">Station ID (station_id)</label>
            <input
              v-model="form.code"
              type="text"
              required
              :readonly="!!stationToEdit"
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white font-mono focus:border-emerald-500 outline-none"
              :class="stationToEdit ? 'opacity-80 cursor-not-allowed' : ''"
              placeholder="e.g. ST-04"
            />
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Station Type (station_type)</label>
            <select
              v-model="form.type"
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:border-emerald-500 outline-none"
            >
              <option value="peripheral">Peripheral (SX1262 LoRa Relay)</option>
              <option value="master">Master Hub (LoRaWAN + Solar)</option>
            </select>
          </div>
        </div>

        <div>
          <label class="block text-slate-300 font-semibold mb-1">Station Name</label>
          <input
            v-model="form.name"
            type="text"
            required
            class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:border-emerald-500 outline-none"
            placeholder="e.g. West Buffer Row Station"
          />
        </div>

        <div>
          <label class="block text-slate-300 font-semibold mb-1">Station Description (description)</label>
          <textarea
            v-model="form.description"
            rows="2"
            class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white focus:border-emerald-500 outline-none"
            placeholder="e.g. Positioned near Paulownia tree PW-RMB-0130 root system for subterranean acoustic monitoring."
          ></textarea>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-slate-300 font-semibold mb-1">Monitored Tree Tag (tree_tag)</label>
            <input
              v-model="form.treeTag"
              type="text"
              required
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-emerald-400 font-mono focus:border-emerald-500 outline-none"
              placeholder="PW-RMB-0130"
            />
          </div>

          <div>
            <label class="block text-slate-300 font-semibold mb-1">Plantation Zone (plantation_zone)</label>
            <input
              v-model="form.plantationZone"
              type="text"
              required
              class="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:border-emerald-500 outline-none"
              placeholder="Zone E - West Border"
            />
          </div>
        </div>

        <!-- Coordinates (lat, lng) -->
        <div class="grid grid-cols-2 gap-4 bg-slate-950/60 p-3 rounded-xl border border-slate-800">
          <div>
            <label class="block text-slate-400 font-medium mb-1">Latitude (coordinates[0])</label>
            <input
              v-model="form.lat"
              type="number"
              step="0.00001"
              required
              class="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-white font-mono focus:border-emerald-500 outline-none"
            />
          </div>

          <div>
            <label class="block text-slate-400 font-medium mb-1">Longitude (coordinates[1])</label>
            <input
              v-model="form.lng"
              type="number"
              step="0.00001"
              required
              class="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-white font-mono focus:border-emerald-500 outline-none"
            />
          </div>
        </div>

        <!-- Buttons -->
        <div class="pt-4 border-t border-slate-800 flex justify-end gap-3">
          <button
            type="button"
            @click="emit('close')"
            class="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 font-semibold transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold flex items-center gap-1.5 transition shadow-lg shadow-emerald-950/50"
          >
            <Save :size="14" />
            <span>Save to Station Information Table</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
