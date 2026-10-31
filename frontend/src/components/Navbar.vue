<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlantationStore } from '@/stores/plantationStore'
import {
  Trees,
  Activity,
  MapPin,
  AlertTriangle,
  Cpu,
  Layers,
  Radio,
  Wifi,
  Bug,
  History
} from '@lucide/vue'

const store = usePlantationStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { name: 'Dashboard Overview', path: '/', icon: Activity },
  { name: 'Plantation GIS Map', path: '/stations', icon: MapPin },
  { name: 'Telemetry Events', path: '/events', icon: History },
  { name: 'Response SOP & Alerts', path: '/alerts', icon: AlertTriangle, badge: true },
  { name: 'Predictive Analytics', path: '/analytics', icon: Cpu },
  { name: 'System Architecture', path: '/architecture', icon: Layers },
]

const activeAlertCount = computed(() => store.activeAlerts.length)
</script>

<template>
  <header class="border-b border-slate-800 bg-slate-900/90 backdrop-blur sticky top-0 z-50">
    <!-- Top Micro Bar -->
    <div class="px-4 py-1.5 bg-slate-950/80 border-b border-slate-800/80 text-xs flex flex-wrap items-center justify-between gap-3 text-slate-400">
      <div class="flex items-center gap-3">
        <span class="inline-flex items-center gap-1.5 text-emerald-400 font-medium">
          <Trees :size="13" />
          Rembau Farm Paulownia Block
        </span>
        <span class="text-slate-600">|</span>
        <span class="text-slate-300">Cadence: <strong>15m Sample &bull; 24h Daily Keepalive</strong></span>
      </div>

      <div class="flex items-center gap-4">
        <!-- Cadence state -->
        <div class="flex items-center gap-2 text-[11px]">
          <span class="px-2 py-0.5 rounded bg-indigo-950 text-indigo-300 border border-indigo-800 font-mono">
            {{ store.dormantStationsCount }} Asleep (24h)
          </span>
          <span class="px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800 font-mono">
            {{ store.triggeredStationsCount }} Active (15m)
          </span>
        </div>

        <!-- Gateway status -->
        <div class="flex items-center gap-1.5">
          <Radio :size="12" class="text-emerald-400" />
          <span>Gateway: <strong class="text-emerald-300">5G Online</strong></span>
        </div>

        <!-- WebSocket Status -->
        <div class="flex items-center gap-1.5">
          <Wifi :size="12" class="text-cyan-400" />
          <span>WebSocket: <strong class="text-cyan-300">Listening</strong></span>
        </div>

        <span class="text-slate-500 text-[11px]">Sync: {{ store.lastSyncTime }}</span>
      </div>
    </div>

    <!-- Main Navigation Bar -->
    <div class="px-4 py-3 flex items-center justify-between gap-4">
      <!-- Brand -->
      <div class="flex items-center gap-3 cursor-pointer" @click="router.push('/')">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-700 flex items-center justify-center shadow-lg shadow-emerald-950/50 text-white font-bold text-lg border border-emerald-400/30">
          <Bug :size="22" class="text-emerald-100" />
        </div>
        <div>
          <div class="flex items-center gap-2">
            <span class="font-extrabold text-base tracking-wide text-white">TERMA-GUARD<span class="text-emerald-400">.AI</span></span>
            <span class="text-[10px] font-semibold uppercase tracking-wider px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800">
              Low-Power IoT
            </span>
          </div>
          <p class="text-[11px] text-slate-400 font-normal">Underground Termite Early-Warning & Moisture-Gated Detection</p>
        </div>
      </div>

      <!-- Nav Links -->
      <nav class="hidden md:flex items-center gap-1">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-medium transition-all"
          :class="[
            route.path === item.path
              ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 shadow-sm'
              : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
          ]"
        >
          <component :is="item.icon" :size="16" />
          <span>{{ item.name }}</span>
          <span
            v-if="item.badge && activeAlertCount > 0"
            class="ml-1 px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-amber-500 text-slate-950"
          >
            {{ activeAlertCount }}
          </span>
        </router-link>
      </nav>
    </div>
  </header>
</template>
