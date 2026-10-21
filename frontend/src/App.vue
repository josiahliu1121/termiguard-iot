<script setup lang="ts">
import { onMounted } from 'vue'
import Navbar from '@/components/Navbar.vue'
import { usePlantationStore } from '@/stores/plantationStore'
import { Trees, ShieldCheck, Heart } from '@lucide/vue'

const store = usePlantationStore()

onMounted(async () => {
  await store.init()
})
</script>

<template>
  <div class="min-h-screen bg-[#090e11] text-slate-100 flex flex-col font-sans selection:bg-emerald-500 selection:text-white">
    <!-- Navbar -->
    <Navbar />

    <!-- Main Content Area -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="border-t border-slate-800/80 bg-slate-950 py-4 px-6 text-xs text-slate-500">
      <div class="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <Trees :size="15" class="text-emerald-500" />
          <span class="text-slate-400 font-medium">Paulownia Afforestation Early-Warning Pilot</span>
          <span>&bull;</span>
          <span>Green Afforestation International Network (Revongen)</span>
        </div>

        <div class="flex items-center gap-4 text-[11px]">
          <span>Integrated Design Project (IDP) 2026</span>
          <span>&bull;</span>
          <span class="text-emerald-400 font-mono">SX1262 LoRa Relay &bull; FastAPI CNN-LSTM &bull; Vue 3</span>
        </div>
      </div>
    </footer>
  </div>
</template>
