import { mockService } from '@/mocks/mockService'
import { mockWebSocket } from '@/mocks/mockWebSocket'
import type { StationInfo, StationEvent, Station, AlertEvent, SystemStatus, VerificationCase, CaseStatus } from '@/types'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1'
const WS_BASE = import.meta.env.VITE_WS_BASE || 'ws://localhost:8080/ws/events'

// Real WebSocket connection state
let realSocket: WebSocket | null = null
const socketSubscribers: Array<(payload: any) => void> = []
let reconnectTimer: ReturnType<typeof setTimeout> | null = null

function connectRealWebSocket() {
  if (realSocket && (realSocket.readyState === WebSocket.OPEN || realSocket.readyState === WebSocket.CONNECTING)) {
    return
  }
  try {
    realSocket = new WebSocket(WS_BASE)
    realSocket.onopen = () => {
      console.log('[TermaGuard] Connected to live WebSocket server:', WS_BASE)
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    }
    realSocket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data)
        socketSubscribers.forEach(cb => cb(payload))
      } catch (e) {
        console.error('[TermaGuard] Failed to parse incoming WebSocket message:', e)
      }
    }
    realSocket.onclose = () => {
      console.warn('[TermaGuard] WebSocket connection closed. Attempting reconnect in 3s...')
      if (!reconnectTimer) {
        reconnectTimer = setTimeout(() => {
          reconnectTimer = null
          connectRealWebSocket()
        }, 3000)
      }
    }
    realSocket.onerror = (err) => {
      console.error('[TermaGuard] WebSocket error:', err)
      realSocket?.close()
    }
  } catch (err) {
    console.error('[TermaGuard] Failed to establish WebSocket connection:', err)
  }
}

export const api = {
  isMock: USE_MOCK,

  // ==========================================
  // TABLE 1: Station Information Table APIs
  // ==========================================
  stations: {
    async getAll(): Promise<Station[]> {
      if (USE_MOCK) return mockService.getStations()
      const res = await fetch(`${API_BASE}/stations`)
      if (!res.ok) throw new Error(`Failed to fetch stations: ${res.statusText}`)
      return res.json()
    },

    async getById(id: string): Promise<Station | null> {
      if (USE_MOCK) return mockService.getStationById(id)
      const res = await fetch(`${API_BASE}/stations/${id}`)
      if (res.status === 404) return null
      if (!res.ok) throw new Error(`Failed to fetch station ${id}: ${res.statusText}`)
      return res.json()
    },

    // MODIFY API: Update station coordinates, description, type, plantation zone, threshold
    async modify(id: string, updates: Partial<StationInfo>): Promise<Station> {
      if (USE_MOCK) return mockService.modifyStationInfo(id, updates)
      const res = await fetch(`${API_BASE}/stations/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates)
      })
      if (!res.ok) throw new Error(`Failed to modify station ${id}: ${res.statusText}`)
      return res.json()
    },

    // Backwards compatibility alias
    async update(id: string, updates: Partial<StationInfo>): Promise<Station> {
      return this.modify(id, updates)
    },

    // Backward compatibility: redirects create to modify (stations are auto-registered upon event capture)
    async create(station: Partial<StationInfo> & { id?: string }): Promise<Station> {
      const id = station.id || station.code || 'ST-00'
      return this.modify(id, station)
    },

    // Human-in-the-Loop Ground Truth Inspection APIs
    async reportFalseNegative(
      stationId: string,
      details: { notes: string; technician?: string; probableCause?: string }
    ): Promise<{ alert: AlertEvent; station: Station; stats: any }> {
      if (USE_MOCK) return mockService.reportFalseNegative(stationId, details)
      const res = await fetch(`${API_BASE}/stations/${stationId}/report-false-negative`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(details)
      })
      if (!res.ok) throw new Error(`Failed to report false negative: ${res.statusText}`)
      return res.json()
    },

    async logRoutineInspection(
      stationId: string,
      details: { notes: string; technician?: string }
    ): Promise<{ station: Station; stats: any }> {
      if (USE_MOCK) return mockService.logRoutineInspection(stationId, details)
      const res = await fetch(`${API_BASE}/stations/${stationId}/routine-inspection`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(details)
      })
      if (!res.ok) throw new Error(`Failed to log routine inspection: ${res.statusText}`)
      return res.json()
    },

    // ==========================================
    // TABLE 2: Event Table APIs
    // ==========================================
    async getEvents(id: string): Promise<StationEvent[]> {
      if (USE_MOCK) return mockService.getStationEvents(id)
      const res = await fetch(`${API_BASE}/stations/${id}/events`)
      if (!res.ok) throw new Error(`Failed to fetch events for station ${id}: ${res.statusText}`)
      return res.json()
    },

    // Backwards compatibility alias
    async getHistory(id: string): Promise<StationEvent[]> {
      return this.getEvents(id)
    }
  },

  events: {
    async getAll(): Promise<StationEvent[]> {
      if (USE_MOCK) return mockService.getAllEvents()
      const res = await fetch(`${API_BASE}/events`)
      if (!res.ok) throw new Error(`Failed to fetch events: ${res.statusText}`)
      return res.json()
    },

    async getByStation(stationId: string): Promise<StationEvent[]> {
      if (USE_MOCK) return mockService.getStationEvents(stationId)
      const res = await fetch(`${API_BASE}/stations/${stationId}/events`)
      if (!res.ok) throw new Error(`Failed to fetch events for station ${stationId}: ${res.statusText}`)
      return res.json()
    },

    async insert(event: Partial<StationEvent>): Promise<StationEvent> {
      if (USE_MOCK) return mockService.insertEvent(event as any)
      const res = await fetch(`${API_BASE}/events`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(event)
      })
      if (!res.ok) throw new Error(`Failed to insert event: ${res.statusText}`)
      return res.json()
    }
  },

  // Alert Management APIs
  alerts: {
    async getAll(): Promise<AlertEvent[]> {
      if (USE_MOCK) return mockService.getAlerts()
      const res = await fetch(`${API_BASE}/alerts`)
      if (!res.ok) throw new Error(`Failed to fetch alerts: ${res.statusText}`)
      return res.json()
    },

    async performAction(
      alertId: string,
      action: 'verify' | 'treat' | 'resolve' | 'mark_false_alarm',
      details: { notes: string; technician?: string; toxicantBatch?: string }
    ): Promise<AlertEvent> {
      if (USE_MOCK) return mockService.processAlertAction(alertId, action, details)
      const res = await fetch(`${API_BASE}/alerts/${alertId}/action`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action, ...details })
      })
      if (!res.ok) throw new Error(`Failed to perform alert action: ${res.statusText}`)
      return res.json()
    }
  },

  // ==========================================
  // TABLE 3: Verification & Response SOP Cases APIs
  // ==========================================
  cases: {
    async getAll(): Promise<VerificationCase[]> {
      if (USE_MOCK) return mockService.getCases()
      const res = await fetch(`${API_BASE}/cases`)
      if (!res.ok) throw new Error(`Failed to fetch cases: ${res.statusText}`)
      return res.json()
    },

    async getById(id: string): Promise<VerificationCase | null> {
      if (USE_MOCK) return mockService.getCaseById(id)
      const res = await fetch(`${API_BASE}/cases/${id}`)
      if (res.status === 404) return null
      if (!res.ok) throw new Error(`Failed to fetch case ${id}: ${res.statusText}`)
      return res.json()
    },

    async getByStation(stationId: string): Promise<VerificationCase[]> {
      if (USE_MOCK) return mockService.getCasesByStation(stationId)
      const res = await fetch(`${API_BASE}/cases/station/${stationId}`)
      if (!res.ok) throw new Error(`Failed to fetch cases for station ${stationId}: ${res.statusText}`)
      return res.json()
    },

    async getActiveByStation(stationId: string): Promise<VerificationCase | null> {
      if (USE_MOCK) return mockService.getActiveCaseForStation(stationId)
      const res = await fetch(`${API_BASE}/cases/active/${stationId}`)
      if (!res.ok) return null
      return res.json()
    },

    async create(data: {
      stationId: string
      finding: 'termites_present' | 'clean_no_termites'
      technician: string
      notes: string
      probableCause?: string
    }): Promise<{ caseRecord: VerificationCase; station: Station; stats: any }> {
      if (USE_MOCK) return mockService.createVerificationCase(data)
      const res = await fetch(`${API_BASE}/cases`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      })
      if (!res.ok) throw new Error(`Failed to create verification case: ${res.statusText}`)
      return res.json()
    },

    async transition(
      caseId: string,
      nextStatus: CaseStatus,
      details: { technician?: string; notes?: string; toxicantBatch?: string }
    ): Promise<{ caseRecord: VerificationCase; station: Station }> {
      if (USE_MOCK) return mockService.transitionVerificationCase(caseId, nextStatus, details)
      const res = await fetch(`${API_BASE}/cases/${caseId}/transition`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nextStatus, ...details })
      })
      if (!res.ok) throw new Error(`Failed to transition case ${caseId}: ${res.statusText}`)
      return res.json()
    },

    async terminate(
      caseId: string,
      details: { technician?: string; notes: string }
    ): Promise<{ caseRecord: VerificationCase; station: Station }> {
      return this.transition(caseId, 'terminated', details)
    }
  },

  // System & Research Metrics
  system: {
    async getStatus(): Promise<SystemStatus> {
      if (USE_MOCK) return mockService.getSystemStatus()
      const res = await fetch(`${API_BASE}/system/status`)
      if (!res.ok) throw new Error(`Failed to fetch system status: ${res.statusText}`)
      return res.json()
    },

    async getGroundTruthMetrics() {
      if (USE_MOCK) return mockService.getGroundTruthMetrics()
      const res = await fetch(`${API_BASE}/analytics/ground-truth-metrics`)
      if (!res.ok) throw new Error(`Failed to fetch ground truth metrics: ${res.statusText}`)
      return res.json()
    }
  },

  // ==========================================
  // WEBSOCKET: Monitors Event Table In Real-Time
  // ==========================================
  telemetrySocket: {
    connect() {
      if (USE_MOCK) {
        mockWebSocket.connect()
      } else {
        connectRealWebSocket()
      }
    },

    disconnect() {
      if (USE_MOCK) {
        mockWebSocket.disconnect()
      } else {
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }
        if (realSocket) {
          realSocket.close()
          realSocket = null
        }
      }
    },

    subscribe(callback: (payload: any) => void) {
      if (USE_MOCK) {
        return mockWebSocket.subscribe(callback)
      }
      socketSubscribers.push(callback)
      return () => {
        const index = socketSubscribers.indexOf(callback)
        if (index !== -1) socketSubscribers.splice(index, 1)
      }
    },

    async advance15MinCycle() {
      if (USE_MOCK) {
        mockWebSocket.advance15MinCycle()
      } else {
        const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
        await api.events.insert({
          stationId: 'ST-02',
          timestamp: now,
          eventType: '15min_triggered_event',
          batteryLevel: 86,
          batteryVoltage: 3.88,
          soilMoisture: 71.2,
          acousticScore: 84,
          temperature: 29.4,
          humidity: 84.0,
          rssi: -88,
          snr: 6.0,
          predictLevel: 'High',
          aiRiskScore: 88,
          isMoistureTriggered: true,
          mfccFeatures: [-10.1, 14.9, 8.5, 6.9, 4.5, 3.3, 2.5, 1.9, 1.3, 1.0, 0.8, 0.5, 0.3]
        })
      }
    },

    async emitDailyHeartbeat(stationId: string = 'ST-00') {
      if (USE_MOCK) {
        mockWebSocket.emitDailyHeartbeat(stationId)
      } else {
        const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
        await api.events.insert({
          stationId,
          timestamp: now,
          eventType: 'daily_heartbeat',
          batteryLevel: 98,
          batteryVoltage: 4.15,
          soilMoisture: 56.2,
          acousticScore: null,
          temperature: 29.5,
          humidity: 78.0,
          rssi: -72,
          snr: 9.4,
          predictLevel: 'Safe',
          aiRiskScore: 10,
          isMoistureTriggered: false
        })
      }
    },

    async triggerMoistureBreach(stationId: string = 'ST-02') {
      if (USE_MOCK) {
        mockWebSocket.triggerMoistureBreach(stationId)
      } else {
        const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
        await api.events.insert({
          stationId,
          timestamp: now,
          eventType: '15min_triggered_event',
          batteryLevel: 85,
          batteryVoltage: 3.86,
          soilMoisture: 73.0,
          acousticScore: 88,
          temperature: 29.6,
          humidity: 85.0,
          rssi: -88,
          snr: 5.8,
          predictLevel: 'High',
          aiRiskScore: 92,
          isMoistureTriggered: true,
          mfccFeatures: [-10.5, 15.2, 8.8, 7.1, 4.7, 3.5, 2.7, 2.0, 1.4, 1.1, 0.9, 0.6, 0.4]
        })
      }
    },

    async resetToBaseline(stationId: string = 'ST-02') {
      if (USE_MOCK) {
        mockWebSocket.resetToBaseline(stationId)
      } else {
        const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
        await api.events.insert({
          stationId,
          timestamp: now,
          eventType: 'daily_heartbeat',
          batteryLevel: 90,
          batteryVoltage: 3.98,
          soilMoisture: 56.0,
          acousticScore: null,
          temperature: 29.0,
          humidity: 78.0,
          rssi: -82,
          snr: 8.0,
          predictLevel: 'Safe',
          aiRiskScore: 10,
          isMoistureTriggered: false
        })
      }
    }
  }
}
