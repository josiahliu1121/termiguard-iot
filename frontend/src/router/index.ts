import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import StationsView from '@/views/StationsView.vue'
import EventsView from '@/views/EventsView.vue'
import AnalyticsView from '@/views/AnalyticsView.vue'
import AlertsView from '@/views/AlertsView.vue'
import ArchitectureView from '@/views/ArchitectureView.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: DashboardView
  },
  {
    path: '/stations',
    name: 'Stations',
    component: StationsView
  },
  {
    path: '/events',
    name: 'Events',
    component: EventsView
  },
  {
    path: '/alerts',
    name: 'Alerts',
    component: AlertsView
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: AnalyticsView
  },
  {
    path: '/architecture',
    name: 'Architecture',
    component: ArchitectureView
  }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})
