<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import L from 'leaflet'
import { usePlantationStore } from '@/stores/plantationStore'
import { GATEWAY_INFO, formatCleanTimeAgo } from '@/mocks/mockData'
import type { Station } from '@/types'
import { Layers, Plus, ZoomIn, Radio, Sun, ShieldAlert } from '@lucide/vue'

const props = defineProps<{
  interactive?: boolean
  heightClass?: string
  stations?: Station[]
}>()

const emit = defineEmits<{
  (e: 'selectStation', station: Station): void
  (e: 'configureStation', station: Station): void
  (e: 'openAddModal'): void
}>()

const store = usePlantationStore()
const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let stationMarkersGroup: L.LayerGroup | null = null
let networkLinesGroup: L.LayerGroup | null = null
let currentTileLayer: L.TileLayer | null = null
const activeMapMode = ref<'satellite' | 'streets'>('satellite')

const tileLayers = {
  satellite: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
  googleHybrid: 'https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',
  dark: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
}

function getTierColor(tier: Station['confidenceTier']) {
  switch (tier) {
    case 'High': return '#ef4444' // red-500
    case 'Medium': return '#f59e0b' // amber-500
    case 'Low': return '#06b6d4' // cyan-500
    default: return '#10b981' // emerald-500
  }
}

function initMap() {
  if (!mapContainer.value) return

  // Center around Rembau Farm pilot block
  map = L.map(mapContainer.value, {
    center: [2.5895, 102.0916],
    zoom: 18,
    maxZoom: 20,
    minZoom: 14,
    zoomControl: false
  })

  // Add default satellite layer
  currentTileLayer = L.tileLayer(tileLayers.satellite, {
    attribution: 'Tiles &copy; Esri &mdash; Rembau Paulownia Block'
  }).addTo(map)

  L.control.zoom({ position: 'bottomright' }).addTo(map)

  stationMarkersGroup = L.layerGroup().addTo(map)
  networkLinesGroup = L.layerGroup().addTo(map)

  renderNetwork()
}

function setTileMode(mode: 'satellite' | 'streets') {
  activeMapMode.value = mode
  if (!map) return

  if (currentTileLayer) {
    map.removeLayer(currentTileLayer)
  }

  const url = mode === 'satellite' ? tileLayers.satellite : tileLayers.dark
  currentTileLayer = L.tileLayer(url, {
    attribution: mode === 'satellite' ? 'Satellite Imagery' : 'CartoDB Dark'
  }).addTo(map)
}

function renderNetwork() {
  if (!map || !stationMarkersGroup || !networkLinesGroup) return

  stationMarkersGroup.clearLayers()
  networkLinesGroup.clearLayers()

  const master = store.masterStation

  // 1. Draw Gateway Tower Marker
  const gatewayIcon = L.divIcon({
    className: 'custom-gw-marker',
    html: `
      <div class="flex items-center justify-center w-8 h-8 rounded-full bg-slate-900 border-2 border-emerald-400 text-emerald-400 shadow-xl shadow-emerald-950/80">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"/><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"/><circle cx="12" cy="12" r="2"/><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"/><path d="M19.1 4.9C23 8.8 23 15.1 19.1 19"/></svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 16]
  })

  const gwMarker = L.marker(GATEWAY_INFO.coordinates, { icon: gatewayIcon })
    .bindPopup(`
      <div class="p-1">
        <div class="text-xs font-bold text-emerald-400 uppercase tracking-wider">Gateway Unit</div>
        <div class="font-bold text-sm text-white">${GATEWAY_INFO.name}</div>
        <div class="text-xs text-slate-300 mt-1">SX1302 LoRaWAN Concentrator</div>
        <div class="text-xs text-slate-400">Backhaul: <span class="text-emerald-400 font-semibold">${GATEWAY_INFO.backhaul} Cellular</span> (${GATEWAY_INFO.signalDbm} dBm)</div>
      </div>
    `, { className: 'dark-popup' })
  stationMarkersGroup.addLayer(gwMarker)

  // 2. Uplink line: Master Station -> Gateway
  if (master) {
    const uplinkLine = L.polyline([master.coordinates, GATEWAY_INFO.coordinates], {
      color: '#10b981',
      weight: 3,
      opacity: 0.85,
      dashArray: '6, 6'
    })
    networkLinesGroup.addLayer(uplinkLine)
  }

  // 3. Draw Station Markers and LoRa P2P Relay lines
  const stationsToRender = props.stations !== undefined ? props.stations : store.stations
  stationsToRender.forEach(station => {
    const isMaster = station.type === 'master'
    const color = getTierColor(station.confidenceTier)
    const isSelected = store.selectedStationId === station.id
    const isHigh = station.confidenceTier === 'High'

    // Draw P2P LoRa line from peripheral to master
    if (!isMaster && master) {
      const relayLine = L.polyline([station.coordinates, master.coordinates], {
        color: '#38bdf8', // cyan-400
        weight: 2,
        opacity: 0.65,
        dashArray: '4, 4'
      })
      networkLinesGroup.addLayer(relayLine)
    }

    // Custom Station Marker HTML
    const markerHtml = `
      <div class="relative flex items-center justify-center cursor-pointer transition-transform hover:scale-110">
        ${isHigh ? `<div class="absolute w-12 h-12 rounded-full bg-rose-500/40 animate-ping"></div>` : ''}
        ${station.isProfileComplete === false ? `<div class="absolute -top-2 -left-2 px-1 py-0.2 bg-indigo-600 rounded-full border border-slate-900 text-[8px] font-black text-white animate-pulse shadow">NEW</div>` : ''}
        <div class="w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs text-white shadow-xl border-2"
          style="background-color: ${color}; border-color: ${isSelected ? '#ffffff' : station.isProfileComplete === false ? '#818cf8' : 'rgba(255,255,255,0.4)'}; box-shadow: 0 0 14px ${color};">
          ${isMaster ? '★' : station.code.replace('ST-0', '').replace('ST-', '')}
        </div>
        ${isMaster ? `<div class="absolute -top-1 -right-1 w-3.5 h-3.5 bg-amber-400 rounded-full border border-slate-900 flex items-center justify-center text-[8px] text-slate-950 font-bold" title="Solar Powered">☀</div>` : ''}
      </div>
    `

    const stationIcon = L.divIcon({
      className: `custom-station-marker station-${station.id}`,
      html: markerHtml,
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    })

    const marker = L.marker(station.coordinates, { icon: stationIcon })
    
    // Popup content
    const popupContent = `
      <div class="p-1 min-w-[210px]">
        <div class="flex items-center justify-between gap-2 border-b border-slate-700 pb-1 mb-2">
          <div class="flex items-center gap-1.5">
            <span class="font-bold text-sm text-white">${station.code}</span>
            ${station.isProfileComplete === false ? `<span class="px-1.5 py-0.2 rounded bg-indigo-950 text-indigo-300 text-[8px] font-bold border border-indigo-700/60 uppercase">New Go-Live</span>` : ''}
          </div>
          <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider" 
            style="background-color: ${color}22; color: ${color}; border: 1px solid ${color}44;">
            ${station.confidenceTier} Tier
          </span>
        </div>
        <div class="text-xs text-slate-300 font-medium mb-1">${station.name}</div>
        <div class="flex items-center justify-between text-[11px] text-slate-400 mb-2">
          <span>Tree Tag: <span class="text-emerald-400 font-mono">${station.treeTag || '<span class="text-indigo-400 italic">Pending Setup</span>'}</span></span>
          <span class="text-[9px] font-bold px-1.5 py-0.5 rounded border"
            style="${(station.humanVerificationStatus === 'verified_infestation' || station.humanVerificationStatus === 'field_verified_infestation') ? 'background:#881337; color:#fda4af; border-color:#f43f5e;' : station.humanVerificationStatus === 'treatment_start' ? 'background:#78350f; color:#fde68a; border-color:#f59e0b;' : station.humanVerificationStatus === 'monitor' ? 'background:#1e1b4b; color:#a5b4fc; border-color:#6366f1;' : station.humanVerificationStatus === 'verified_clean' ? 'background:#064e3b; color:#6ee7b7; border-color:#10b981;' : 'background:#1e293b; color:#94a3b8; border-color:#475569;'}">
            ${station.humanVerificationStatus === 'verified_clean' ? '✓ ' + formatCleanTimeAgo(station.verifiedCleanAt) : (station.humanVerificationStatus === 'field_verified_infestation' || station.humanVerificationStatus === 'verified_infestation') ? '● INFESTATION VERIFIED' : station.humanVerificationStatus === 'treatment_start' ? '● IN TREATMENT' : station.humanVerificationStatus === 'monitor' ? '● MONITORING (40D)' : station.humanVerificationStatus === 'false_alarm' ? '⚠ FALSE ALARM' : 'UNVERIFIED'}
          </span>
        </div>
        
        <div class="grid grid-cols-2 gap-1.5 text-xs bg-slate-900/80 p-2 rounded border border-slate-800 mb-2">
          <div>Activity Score: <strong class="text-white">${station.activityScore}/100</strong></div>
          <div>Soil Moisture: <strong class="text-cyan-300">${station.soilMoisture}%</strong></div>
          <div>Microclimate: <strong class="text-slate-200">${station.temperature}°C / ${station.humidity}%</strong></div>
          <div>Battery: <strong class="text-emerald-300">${station.batteryLevel}%</strong></div>
        </div>

        <div class="mb-2 p-1.5 rounded bg-slate-950/80 border border-slate-800 text-[11px]">
          <div class="flex justify-between items-center mb-0.5">
            <span class="text-slate-400">Rolling Threat Index (ITI):</span>
            <span class="font-bold font-mono" style="color: ${color};">${station.threatIndex ?? 0}/100</span>
          </div>
          <div class="text-[10px] text-slate-300 truncate">${station.windowSummary || 'Temporal evaluation active'}</div>
        </div>

        ${station.isProfileComplete === false ? `
          <button id="btn-config-${station.id}" class="w-full py-1 mb-1.5 text-center bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-semibold flex items-center justify-center gap-1 shadow">
            <span>⚙ Key In Station Profile</span>
          </button>
        ` : ''}

        <button id="btn-select-${station.id}" class="w-full py-1 text-center bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold">
          Select & View Telemetry
        </button>
      </div>
    `

    marker.bindPopup(popupContent, { className: 'dark-popup' })

    marker.on('popupopen', () => {
      const configBtn = document.getElementById(`btn-config-${station.id}`)
      if (configBtn) {
        configBtn.onclick = () => {
          emit('configureStation', station)
        }
      }

      const btn = document.getElementById(`btn-select-${station.id}`)
      if (btn) {
        btn.onclick = () => {
          store.selectStation(station.id)
          emit('selectStation', station)
        }
      }
    })

    marker.on('click', () => {
      store.selectStation(station.id)
      emit('selectStation', station)
    })

    stationMarkersGroup.addLayer(marker)
  })
}

function resetView() {
  if (!map) return
  map.setView([2.5895, 102.0916], 18)
}

watch(() => store.stations, () => {
  renderNetwork()
}, { deep: true })

watch(() => props.stations, () => {
  renderNetwork()
}, { deep: true })

watch(() => store.selectedStationId, () => {
  renderNetwork()
})

onMounted(() => {
  nextTick(() => {
    initMap()
  })
})
</script>

<template>
  <div class="relative z-0 isolate w-full rounded-2xl overflow-hidden border border-slate-800 shadow-2xl bg-slate-950 flex flex-col" :class="heightClass || 'h-[460px]'">
    <!-- Top Overlay Map Controls -->
    <div class="absolute top-3 left-3 z-10 flex items-center gap-2">
      <div class="bg-slate-900/90 backdrop-blur px-3 py-1.5 rounded-lg border border-slate-700 text-xs shadow-lg flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
        <span class="font-semibold text-white">GIS Plantation Map</span>
        <span class="text-slate-400 text-[11px]">(Rembau Farm Block A)</span>
      </div>

      <div class="bg-slate-900/90 backdrop-blur p-1 rounded-lg border border-slate-700 text-xs shadow-lg flex items-center gap-1">
        <button
          @click="setTileMode('satellite')"
          class="px-2.5 py-1 rounded text-[11px] font-medium transition"
          :class="activeMapMode === 'satellite' ? 'bg-emerald-600 text-white font-semibold' : 'text-slate-400 hover:text-white'"
        >
          Satellite
        </button>
        <button
          @click="setTileMode('streets')"
          class="px-2.5 py-1 rounded text-[11px] font-medium transition"
          :class="activeMapMode === 'streets' ? 'bg-emerald-600 text-white font-semibold' : 'text-slate-400 hover:text-white'"
        >
          Dark Map
        </button>
      </div>
    </div>

    <!-- Right Overlay Controls -->
    <div class="absolute top-3 right-3 z-10 flex items-center gap-2">
      <button
        @click="emit('openAddModal')"
        class="bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-1.5 rounded-lg text-xs font-semibold shadow-lg flex items-center gap-1.5 transition"
      >
        <Plus :size="14" />
        <span>Add Station</span>
      </button>

      <button
        @click="resetView"
        class="bg-slate-900/90 hover:bg-slate-800 text-slate-200 p-2 rounded-lg border border-slate-700 text-xs shadow-lg transition"
        title="Reset Map View"
      >
        <ZoomIn :size="14" />
      </button>
    </div>

    <!-- Bottom Map Legend -->
    <div class="absolute bottom-3 left-3 z-10 bg-slate-950/90 backdrop-blur p-2.5 rounded-xl border border-slate-800 text-[11px] text-slate-300 shadow-xl flex flex-wrap items-center gap-3">
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
        <span>Safe / Baseline</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-cyan-500"></span>
        <span>Low Tier</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
        <span>Medium Alert</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping"></span>
        <span>High Escalation</span>
      </div>
      <span class="text-slate-600">|</span>
      <div class="flex items-center gap-1.5">
        <span class="w-3.5 h-0.5 border-t-2 border-dashed border-cyan-400"></span>
        <span>LoRa P2P Relay</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-3.5 h-0.5 border-t-2 border-dashed border-emerald-400"></span>
        <span>LoRaWAN Uplink</span>
      </div>
    </div>

    <!-- Map Canvas -->
    <div ref="mapContainer" class="w-full flex-1"></div>
  </div>
</template>
