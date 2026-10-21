import { mockService } from '@/mocks/mockService'
import { mockWebSocket } from '@/mocks/mockWebSocket'
import type { StationInfo, StationEvent, Station, AlertEvent, ForecastModelMetrics, SystemStatus, VerificationCase, CaseStatus } from '@/types'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1'

export const api = {
  isMock: USE_MOCK,

  // ==========================================
  // TABLE 1: Station Information Table APIs
  // ==========================================
  stations: {
    async getAll(): Promise<Station[]> {
      if (USE_MOCK) return mockService.getStations()
      const res = await fetch(`${API_BASE}/stations`)
      return res.json()
    },

    async getById(id: string): Promise<Station | null> {
      if (USE_MOCK) return mockService.getStationById(id)
      const res = await fetch(`${API_BASE}/stations/${id}`)
      return res.json()
    },

    // MODIFY API: Update station coordinates, description, type, plantation zone
    async modify(id: string, updates: Partial<StationInfo>): Promise<Station> {
      if (USE_MOCK) return mockService.modifyStationInfo(id, updates)
      const res = await fetch(`${API_BASE}/stations/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates)
      })
      return res.json()
    },

    async create(station: Omit<StationInfo, 'id'>): Promise<Station> {
      if (USE_MOCK) return mockService.addStationInfo(station)
      const res = await fetch(`${API_BASE}/stations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(station)
      })
      return res.json()
    },

    // Backwards compatibility alias
    async update(id: string, updates: Partial<StationInfo>): Promise<Station> {
      return this.modify(id, updates)
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
      return res.json()
    },

    // ==========================================
    // TABLE 2: Event Table APIs
    // ==========================================
    async getEvents(id: string): Promise<StationEvent[]> {
      if (USE_MOCK) return mockService.getStationEvents(id)
      const res = await fetch(`${API_BASE}/stations/${id}/events`)
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
      return res.json()
    },

    async getByStation(stationId: string): Promise<StationEvent[]> {
      if (USE_MOCK) return mockService.getStationEvents(stationId)
      const res = await fetch(`${API_BASE}/stations/${stationId}/events`)
      return res.json()
    }
  },

  // Alert Management APIs
  alerts: {
    async getAll(): Promise<AlertEvent[]> {
      if (USE_MOCK) return mockService.getAlerts()
      const res = await fetch(`${API_BASE}/alerts`)
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
      return res.json()
    },

    async getById(id: string): Promise<VerificationCase | null> {
      if (USE_MOCK) return mockService.getCaseById(id)
      const res = await fetch(`${API_BASE}/cases/${id}`)
      return res.json()
    },

    async getByStation(stationId: string): Promise<VerificationCase[]> {
      if (USE_MOCK) return mockService.getCasesByStation(stationId)
      const res = await fetch(`${API_BASE}/cases/station/${stationId}`)
      return res.json()
    },

    async getActiveByStation(stationId: string): Promise<VerificationCase | null> {
      if (USE_MOCK) return mockService.getActiveCaseForStation(stationId)
      const res = await fetch(`${API_BASE}/cases/active/${stationId}`)
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
    async getMetrics(): Promise<ForecastModelMetrics> {
      if (USE_MOCK) return mockService.getSystemMetrics()
      const res = await fetch(`${API_BASE}/analytics/metrics`)
      return res.json()
    },

    async getStatus(): Promise<SystemStatus> {
      if (USE_MOCK) return mockService.getSystemStatus()
      const res = await fetch(`${API_BASE}/system/status`)
      return res.json()
    },

    async getGroundTruthMetrics() {
      if (USE_MOCK) return mockService.getGroundTruthMetrics()
      const res = await fetch(`${API_BASE}/analytics/ground-truth-metrics`)
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
        // Real WebSocket client connects to ws://localhost:8080/ws/events
      }
    },

    disconnect() {
      if (USE_MOCK) {
        mockWebSocket.disconnect()
      }
    },

    subscribe(callback: (payload: any) => void) {
      if (USE_MOCK) {
        return mockWebSocket.subscribe(callback)
      }
      return () => {}
    },

    advance15MinCycle() {
      if (USE_MOCK) mockWebSocket.advance15MinCycle()
    },

    emitDailyHeartbeat(stationId?: string) {
      if (USE_MOCK) mockWebSocket.emitDailyHeartbeat(stationId)
    },

    triggerMoistureBreach(stationId?: string) {
      if (USE_MOCK) mockWebSocket.triggerMoistureBreach(stationId)
    },

    resetToBaseline(stationId?: string) {
      if (USE_MOCK) mockWebSocket.resetToBaseline(stationId)
    }
  }
}
