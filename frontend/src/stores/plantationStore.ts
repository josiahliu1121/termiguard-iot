import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api'
import { FORECAST_MODEL_METRICS } from '@/mocks/mockData'
import type { Station, StationInfo, StationEvent, AlertEvent, ForecastModelMetrics, SystemStatus, VerificationCase, CaseStatus } from '@/types'

export const usePlantationStore = defineStore('plantation', () => {
  const stations = ref<Station[]>([])
  const alerts = ref<AlertEvent[]>([])
  const cases = ref<VerificationCase[]>([])
  const metrics = ref<ForecastModelMetrics>(FORECAST_MODEL_METRICS)
  const systemStatus = ref<SystemStatus | null>(null)
  const selectedStationId = ref<string>('ST-02')
  const liveEvents = ref<StationEvent[]>([])
  const allEvents = ref<StationEvent[]>([])
  const isLoading = ref<boolean>(false)
  const lastSyncTime = ref<string>('Connecting...')
  const isSimulatingBreach = ref<boolean>(false)

  // Computed
  const unconfiguredStations = computed(() => {
    return stations.value.filter(s => s.isProfileComplete === false)
  })

  const selectedStation = computed(() => {
    return stations.value.find(s => s.id === selectedStationId.value) || stations.value[0] || null
  })

  const masterStation = computed(() => {
    return stations.value.find(s => s.type === 'master') || null
  })

  const peripheralStations = computed(() => {
    return stations.value.filter(s => s.type === 'peripheral')
  })

  const dormantStationsCount = computed(() => {
    return stations.value.filter(s => s.isAcousticGated).length
  })

  const triggeredStationsCount = computed(() => {
    return stations.value.filter(s => !s.isAcousticGated).length
  })

  const activeAlerts = computed(() => {
    return alerts.value.filter(a => a.status !== 'resolved' && a.status !== 'false_alarm')
  })

  const activeCases = computed(() => {
    return cases.value.filter(c =>
      c.status !== 'cleared' &&
      c.status !== 'routine_clean' &&
      c.status !== 'false_alarm' &&
      c.status !== 'terminated'
    )
  })

  function getActiveCaseForStation(stationId: string): VerificationCase | undefined {
    return cases.value.find(c =>
      c.stationId === stationId &&
      c.status !== 'cleared' &&
      c.status !== 'routine_clean' &&
      c.status !== 'false_alarm' &&
      c.status !== 'terminated'
    )
  }

  const overallThreatLevel = computed(() => {
    if (stations.value.some(s => s.threatLevel === 'High' || s.confidenceTier === 'High')) return 'High'
    if (stations.value.some(s => s.threatLevel === 'Medium' || s.confidenceTier === 'Medium')) return 'Medium'
    if (stations.value.some(s => s.threatLevel === 'Low' || s.confidenceTier === 'Low')) return 'Low'
    return 'Safe'
  })

  const averageThreatIndex = computed(() => {
    if (!stations.value.length) return 0
    const sum = stations.value.reduce((acc, s) => acc + (s.threatIndex || 0), 0)
    return Math.round(sum / stations.value.length)
  })

  const averageMoisture = computed(() => {
    if (!stations.value.length) return 0
    const sum = stations.value.reduce((acc, s) => acc + s.soilMoisture, 0)
    return Number((sum / stations.value.length).toFixed(1))
  })

  // ==========================================
  // Store Actions
  // ==========================================
  async function init() {
    isLoading.value = true
    try {
      const [fetchedStations, fetchedAlerts, fetchedStatus, fetchedEvents, fetchedCases] = await Promise.all([
        api.stations.getAll(),
        api.alerts.getAll(),
        api.system.getStatus(),
        api.events.getAll(),
        api.cases.getAll()
      ])

      stations.value = fetchedStations
      alerts.value = fetchedAlerts
      systemStatus.value = fetchedStatus
      allEvents.value = fetchedEvents
      liveEvents.value = fetchedEvents.slice(0, 5)
      cases.value = fetchedCases
      lastSyncTime.value = new Date().toLocaleTimeString()

      // WEBSOCKET: Monitors Event Table inserts & Station Table status updates
      api.telemetrySocket.connect()
      api.telemetrySocket.subscribe((payload: any) => {
        if (payload.type === 'event_inserted' && payload.event) {
          const event: StationEvent = payload.event
          
          // Prepend to allEvents table state
          allEvents.value.unshift(event)

          // Push into live events queue
          liveEvents.value.unshift(event)
          if (liveEvents.value.length > 25) liveEvents.value.pop()
          lastSyncTime.value = new Date().toLocaleTimeString()
        } else if (payload.type === 'station_updated' && payload.station) {
          // Live Station Information Table update pushed via WebSocket!
          const updatedStation: Station = payload.station
          const idx = stations.value.findIndex(s => s.id === updatedStation.id)
          if (idx !== -1) {
            stations.value[idx] = updatedStation
          }
          lastSyncTime.value = new Date().toLocaleTimeString()
        } else if (payload.type === 'alert_triggered' && payload.alert) {
          const newAlert: AlertEvent = payload.alert
          if (!alerts.value.some(a => a.id === newAlert.id)) {
            alerts.value.unshift(newAlert)
          }
        }
      })
    } catch (err) {
      console.error('Failed to initialize plantation data:', err)
    } finally {
      isLoading.value = false
    }
  }

  async function selectStation(id: string) {
    selectedStationId.value = id
  }

  // MODIFY API: Updates Station Information Table (coordinates, description, type, zone)
  async function updateStationInfo(id: string, updates: Partial<StationInfo>) {
    const updated = await api.stations.modify(id, updates)
    const idx = stations.value.findIndex(s => s.id === id)
    if (idx !== -1) {
      stations.value[idx] = updated
    } else {
      stations.value.push(updated)
    }
    return updated
  }

  // Alias for backward compatibility
  async function updateStationDetails(id: string, updates: Partial<StationInfo>) {
    return updateStationInfo(id, updates)
  }

  // Completes / modifies station profile via PUT /api/v1/stations/{id}
  async function addNewStation(data: Partial<StationInfo>) {
    const id = data.id || data.code || (stations.value.length ? `ST-0${stations.value.length}` : 'ST-00')
    return updateStationInfo(id, data)
  }

  // SOP Response Workflow Actions
  async function handleSopAction(
    alertId: string,
    action: 'verify' | 'treat' | 'resolve' | 'mark_false_alarm',
    details: { notes: string; technician?: string; toxicantBatch?: string }
  ) {
    const updatedAlert = await api.alerts.performAction(alertId, action, details)
    const idx = alerts.value.findIndex(a => a.id === alertId)
    if (idx !== -1) {
      alerts.value[idx] = updatedAlert
    }
    // Refresh stations and allEvents to reflect ground truth labeling
    const [freshStations, freshEvents] = await Promise.all([
      api.stations.getAll(),
      api.events.getAll()
    ])
    stations.value = freshStations
    allEvents.value = freshEvents
    return updatedAlert
  }

  // Report False Negative (Human discovers termites on safe station)
  async function reportFalseNegative(
    stationId: string,
    details: { notes: string; technician?: string; probableCause?: string }
  ) {
    const result = await api.stations.reportFalseNegative(stationId, details)
    if (result.alert && !alerts.value.some(a => a.id === result.alert.id)) {
      alerts.value.unshift(result.alert)
    }
    const [freshStations, freshEvents] = await Promise.all([
      api.stations.getAll(),
      api.events.getAll()
    ])
    stations.value = freshStations
    allEvents.value = freshEvents
    return result
  }

  // Log Routine Inspection (Human confirms clean station - TN)
  async function logRoutineInspection(
    stationId: string,
    details: { notes: string; technician?: string }
  ) {
    const result = await api.stations.logRoutineInspection(stationId, details)
    const [freshStations, freshEvents] = await Promise.all([
      api.stations.getAll(),
      api.events.getAll()
    ])
    stations.value = freshStations
    allEvents.value = freshEvents
    return result
  }

  // ==========================================
  // TABLE 3: Verification & Response Cases Actions
  // ==========================================
  async function loadCases() {
    cases.value = await api.cases.getAll()
  }

  async function createVerificationCase(data: {
    stationId: string
    finding: 'termites_present' | 'clean_no_termites'
    technician: string
    notes: string
    probableCause?: string
  }) {
    const result = await api.cases.create(data)
    const [freshStations, freshEvents, freshAlerts, freshCases] = await Promise.all([
      api.stations.getAll(),
      api.events.getAll(),
      api.alerts.getAll(),
      api.cases.getAll()
    ])
    stations.value = freshStations
    allEvents.value = freshEvents
    alerts.value = freshAlerts
    cases.value = freshCases
    return result
  }

  async function transitionVerificationCase(
    caseId: string,
    nextStatus: CaseStatus,
    details: { technician?: string; notes?: string; toxicantBatch?: string }
  ) {
    const result = await api.cases.transition(caseId, nextStatus, details)
    const [freshStations, freshEvents, freshAlerts, freshCases] = await Promise.all([
      api.stations.getAll(),
      api.events.getAll(),
      api.alerts.getAll(),
      api.cases.getAll()
    ])
    stations.value = freshStations
    allEvents.value = freshEvents
    alerts.value = freshAlerts
    cases.value = freshCases
    return result
  }

  async function terminateVerificationCase(
    caseId: string,
    details: { technician?: string; notes: string }
  ) {
    return transitionVerificationCase(caseId, 'terminated', details)
  }

  // Live Ground Truth Confusion Matrix & Performance Metrics
  const groundTruthStats = computed(() => {
    let tp = 0, fp = 0, fn = 0, tn = 0
    for (const ev of allEvents.value) {
      if (ev.validationOutcome === 'TP') tp++
      else if (ev.validationOutcome === 'FP') fp++
      else if (ev.validationOutcome === 'FN') fn++
      else if (ev.validationOutcome === 'TN') tn++
    }
    const total = tp + fp + fn + tn
    const precision = (tp + fp) > 0 ? Number(((tp / (tp + fp)) * 100).toFixed(1)) : 100
    const recall = (tp + fn) > 0 ? Number(((tp / (tp + fn)) * 100).toFixed(1)) : 100
    const accuracy = total > 0 ? Number((((tp + tn) / total) * 100).toFixed(1)) : 100
    const falseAlarmRate = (fp + tn) > 0 ? Number(((fp / (fp + tn)) * 100).toFixed(1)) : 0
    const missRate = (tp + fn) > 0 ? Number(((fn / (tp + fn)) * 100).toFixed(1)) : 0

    return { tp, fp, fn, tn, total, precision, recall, accuracy, falseAlarmRate, missRate }
  })

  // Simulation Triggers
  function advance15MinPoll() {
    api.telemetrySocket.advance15MinCycle()
  }

  function emitHeartbeat(stationId?: string) {
    api.telemetrySocket.emitDailyHeartbeat(stationId || selectedStationId.value)
  }

  function toggleMoistureBreach(stationId?: string) {
    const targetId = stationId || selectedStationId.value
    const targetStation = stations.value.find(s => s.id === targetId)

    if (targetStation && !targetStation.isAcousticGated) {
      isSimulatingBreach.value = false
      api.telemetrySocket.resetToBaseline(targetId)
    } else {
      isSimulatingBreach.value = true
      api.telemetrySocket.triggerMoistureBreach(targetId)
    }
  }

  async function simulateNewStationDiscovery(stationId: string = 'ST-05') {
    if (api.isMock) {
      const { mockWebSocket } = await import('@/mocks/mockWebSocket')
      const created = await mockWebSocket.simulateNewStationDiscovery(stationId)
      if (created && !stations.value.some(s => s.id === created.id)) {
        stations.value.push(created)
      }
      return created
    } else {
      const existing = stations.value.find(s => s.id === stationId)
      if (existing) return existing

      const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
      // Capturing telemetry automatically auto-registers the station record on the backend
      await api.events.insert({
        stationId,
        timestamp: now,
        eventType: 'daily_heartbeat',
        batteryLevel: 98,
        batteryVoltage: 4.14,
        soilMoisture: 67.4,
        acousticScore: 54,
        temperature: 29.2,
        humidity: 81.0,
        rssi: -79,
        snr: 8.4,
        predictLevel: 'Low',
        aiRiskScore: 48,
        isMoistureTriggered: true,
        mfccFeatures: [-8.5, 11.2, 6.4, 4.8, 3.1, 2.0, 1.5, 1.1, 0.8, 0.5, 0.4, 0.2, 0.1]
      })

      // Fetch newly created station or wait for websocket push
      const created = await api.stations.getById(stationId)
      if (created && !stations.value.some(s => s.id === created.id)) {
        stations.value.push(created)
      }
      return created
    }
  }

  return {
    stations,
    alerts,
    cases,
    activeCases,
    getActiveCaseForStation,
    unconfiguredStations,
    metrics,
    systemStatus,
    selectedStationId,
    selectedStation,
    masterStation,
    peripheralStations,
    dormantStationsCount,
    triggeredStationsCount,
    activeAlerts,
    overallThreatLevel,
    averageThreatIndex,
    averageMoisture,
    groundTruthStats,
    liveEvents,
    livePackets: liveEvents, // backwards compatibility
    allEvents,
    loadAllEvents: async () => {
      allEvents.value = await api.events.getAll()
    },
    loadCases,
    isLoading,
    lastSyncTime,
    isSimulatingBreach,
    init,
    selectStation,
    updateStationInfo,
    updateStationDetails,
    addNewStation,
    handleSopAction,
    reportFalseNegative,
    logRoutineInspection,
    createVerificationCase,
    transitionVerificationCase,
    terminateVerificationCase,
    advance15MinPoll,
    emitHeartbeat,
    toggleMoistureBreach,
    simulateNewStationDiscovery
  }
})
