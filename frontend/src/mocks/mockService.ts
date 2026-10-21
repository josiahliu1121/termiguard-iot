import {
  STATIONS_TABLE,
  createInitialEventsTable,
  composeStation,
  INITIAL_ALERTS,
  SYSTEM_METRICS,
  INITIAL_SYSTEM_STATUS,
  INITIAL_CASES
} from './mockData'
import type { StationInfo, StationEvent, Station, AlertEvent, ForecastModelMetrics, SystemStatus, PredictLevel, VerificationCase, CaseStatus } from '@/types'

// ==========================================
// In-Memory Database Storage (3 Tables)
// 1. stationsTable (Station Info)
// 2. eventsTable (Telemetry Events)
// 3. casesTable (Human Verification & SOP Cases)
// ==========================================
let stationsTable: StationInfo[] = JSON.parse(JSON.stringify(STATIONS_TABLE))
let eventsTable: StationEvent[] = createInitialEventsTable()
let alertsTable: AlertEvent[] = JSON.parse(JSON.stringify(INITIAL_ALERTS))
let casesTable: VerificationCase[] = JSON.parse(JSON.stringify(INITIAL_CASES))
let systemStatus: SystemStatus = { ...INITIAL_SYSTEM_STATUS }

const delay = (ms = 60) => new Promise(resolve => setTimeout(resolve, ms))

// Helper to get latest event for a station from events table
function getLatestEventForStation(stationId: string): StationEvent | undefined {
  const stationEvents = eventsTable.filter(e => e.stationId === stationId)
  if (!stationEvents.length) return undefined
  return stationEvents[stationEvents.length - 1]
}

export const mockService = {
  // =========================================================================
  // TABLE 1 APIs: Station Information Table (stations)
  // =========================================================================

  // GET /api/v1/stations (Returns station info joined with latest event)
  async getStations(): Promise<Station[]> {
    await delay(50)
    return stationsTable.map(st => {
      const latest = getLatestEventForStation(st.id)
      return composeStation(st, latest)
    })
  },

  // GET /api/v1/stations/{id}
  async getStationById(id: string): Promise<Station | null> {
    await delay(30)
    const info = stationsTable.find(s => s.id === id)
    if (!info) return null
    const latest = getLatestEventForStation(id)
    return composeStation(info, latest)
  },

  // PUT /api/v1/stations/{id} (MODIFY API for Station Information Table)
  async modifyStationInfo(id: string, updates: Partial<StationInfo>): Promise<Station> {
    await delay(80)
    const idx = stationsTable.findIndex(s => s.id === id)
    if (idx === -1) throw new Error(`Station ${id} not found in stations table`)

    // Update only station information table attributes
    stationsTable[idx] = {
      ...stationsTable[idx],
      ...updates
    }

    const latest = getLatestEventForStation(id)
    return composeStation(stationsTable[idx], latest)
  },

  // POST /api/v1/stations (Adds new station to station info table)
  async addStationInfo(newStation: Partial<StationInfo>): Promise<Station> {
    await delay(100)
    const id = newStation.id || `ST-${String(stationsTable.length).padStart(2, '0')}`
    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const createdInfo: StationInfo = {
      id,
      code: id,
      name: newStation.name || `Station ${id}`,
      description: newStation.description || 'Auto-registered from initial telemetry packet.',
      type: newStation.type || 'peripheral',
      coordinates: newStation.coordinates || [2.5898, 102.0931],
      plantationZone: newStation.plantationZone || '',
      treeTag: newStation.treeTag || '',
      isProfileComplete: newStation.isProfileComplete ?? false,
      moistureThreshold: newStation.moistureThreshold || 65.0,
      samplingFrequencyMinutes: newStation.samplingFrequencyMinutes || 15,
      heartbeatIntervalHours: newStation.heartbeatIntervalHours || 24,
      isSolarPowered: newStation.type === 'master',
      installedAt: newStation.installedAt || now,
      threatIndex: newStation.threatIndex ?? 10,
      threatLevel: newStation.threatLevel ?? 'Safe',
      windowHits: newStation.windowHits ?? 0,
      windowTotal: newStation.windowTotal ?? 1,
      windowSummary: newStation.windowSummary || 'Initial telemetry online',
      lastEvaluatedAt: now,
      humanVerificationStatus: 'unverified'
    }
    stationsTable.push(createdInfo)

    // Insert initial keepalive event into events table
    const initialEvent: StationEvent = {
      id: `evt-${id}-init`,
      stationId: id,
      timestamp: createdInfo.installedAt,
      eventType: 'daily_heartbeat',
      batteryLevel: 100,
      batteryVoltage: 4.2,
      soilMoisture: 58.0,
      acousticScore: null,
      temperature: 29.0,
      humidity: 78.0,
      rssi: -82,
      snr: 8.0,
      predictLevel: 'Safe',
      aiRiskScore: 10,
      isMoistureTriggered: false
    }
    eventsTable.push(initialEvent)

    return composeStation(createdInfo, initialEvent)
  },

  // =========================================================================
  // TEMPORAL EVALUATION ENGINE: Option A + B (Rolling Window + Threat Index ITI)
  // Evaluates recent events and updates the Station Information Table directly
  // =========================================================================
  evaluateAndUpdateStationThreat(stationId: string): Station {
    const stIdx = stationsTable.findIndex(s => s.id === stationId)
    if (stIdx === -1) throw new Error(`Station ${stationId} not found`)

    const stInfo = stationsTable[stIdx]
    // Get recent events for this station, ordered chronologically
    const stEvents = eventsTable
      .filter(e => e.stationId === stationId)
      .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())

    // Option A: Sliding window of last N = 6 cycles (representing 1.5 hours of 15m polling)
    const WINDOW_SIZE = 6
    const recentWindow = stEvents.slice(-WINDOW_SIZE)
    const windowTotal = Math.min(recentWindow.length, WINDOW_SIZE)

    // Count persistence criteria
    let windowHits = 0
    let moistureElevatedCount = 0

    // Option B: Calculate rolling exponential decay score (ITI: 0 - 100)
    let currentScore = 15 // baseline quiet score
    const LAMBDA = 0.82 // exponential time-decay factor

    for (const ev of recentWindow) {
      const isMoist = ev.soilMoisture >= stInfo.moistureThreshold
      const hasAcoustic = ev.acousticScore !== null && ev.acousticScore >= 50

      if (isMoist) moistureElevatedCount++
      if (isMoist && hasAcoustic) windowHits++

      // Compute step delta S
      let deltaS = 0
      if (ev.eventType === 'daily_heartbeat' || !isMoist) {
        deltaS = -15 // Rapid recovery decay when normal or deep asleep
      } else if (hasAcoustic && ev.acousticScore! >= 75) {
        deltaS = +28 // High confidence acoustic chew burst
      } else if (hasAcoustic && ev.acousticScore! >= 50) {
        deltaS = +20 // Moderate acoustic activity
      } else {
        deltaS = +6 // Moisture elevated without acoustic (cautious watch)
      }

      currentScore = Math.max(0, Math.min(100, Math.round(LAMBDA * currentScore + deltaS)))
    }

    // Determine Aggregated Threat Level (combines ITI + M-of-N persistence)
    let threatLevel: PredictLevel = 'Safe'
    let windowSummary = ''

    if (windowHits >= 4 || (currentScore >= 80 && windowHits >= 3)) {
      threatLevel = 'High'
      windowSummary = `${windowHits}/${windowTotal} active detections in past 90 mins (Confirmed Infestation)`
    } else if (windowHits >= 2 || (currentScore >= 55 && windowHits >= 1)) {
      threatLevel = 'Medium'
      windowSummary = `${windowHits}/${windowTotal} active detections in past 90 mins (Suspected Active Foraging)`
    } else if (moistureElevatedCount >= 1 || currentScore >= 30) {
      threatLevel = 'Low'
      windowSummary = `${moistureElevatedCount}/${windowTotal} moisture elevated cycles (Under Observation)`
    } else {
      threatLevel = 'Safe'
      windowSummary = `0/${windowTotal || 1} triggers - Gated Asleep (Normal Baseline)`
    }

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

    // WRITE DIRECTLY TO STATION INFORMATION TABLE
    stationsTable[stIdx] = {
      ...stInfo,
      threatIndex: currentScore,
      threatLevel,
      windowHits,
      windowTotal: WINDOW_SIZE,
      windowSummary,
      lastEvaluatedAt: now
    }

    const latest = getLatestEventForStation(stationId)
    return composeStation(stationsTable[stIdx], latest)
  },

  // =========================================================================
  // TABLE 2 APIs: Event Table (events)
  // =========================================================================

  // GET /api/v1/events (Retrieve all events from Event Table)
  async getAllEvents(): Promise<StationEvent[]> {
    await delay(50)
    return JSON.parse(JSON.stringify(eventsTable))
      .sort((a: StationEvent, b: StationEvent) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
  },

  // GET /api/v1/stations/{id}/events
  async getStationEvents(stationId: string): Promise<StationEvent[]> {
    await delay(60)
    return eventsTable
      .filter(e => e.stationId === stationId)
      .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
  },

  // POST /api/v1/events (Insert new telemetry record into Event Table)
  async insertEvent(eventData: Omit<StationEvent, 'id'>): Promise<StationEvent> {
    const id = `evt-${Date.now()}-${Math.floor(Math.random() * 1000)}`
    const newEvent: StationEvent = {
      id,
      ...eventData
    }
    eventsTable.push(newEvent)
    return newEvent
  },

  // =========================================================================
  // Alerts & SOP Workflow
  // =========================================================================
  async getAlerts(): Promise<AlertEvent[]> {
    await delay(50)
    return JSON.parse(JSON.stringify(alertsTable))
  },

  // =========================================================================
  // GROUND-TRUTH RETROACTIVE 7-DAY AUTO-LABELING ENGINE
  // Automatically labels events in the past 7 days based on human field inspection
  // Updates humanVerificationStatus without tampering with mathematical threatIndex
  // =========================================================================
  applyGroundTruthValidation(
    stationId: string,
    finding: 'termites_present' | 'clean_no_termites',
    technician: string = 'Field Technician',
    notes: string = ''
  ): { labeledCount: number; tp: number; fp: number; fn: number; tn: number; station: Station } {
    const stIdx = stationsTable.findIndex(s => s.id === stationId)
    if (stIdx === -1) throw new Error(`Station ${stationId} not found`)

    const nowIso = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const nowTime = new Date().getTime()
    const lastVerTime = stationsTable[stIdx].lastVerifiedAt
      ? new Date(stationsTable[stIdx].lastVerifiedAt!).getTime()
      : 0
    const sevenDaysAgo = nowTime - 7 * 24 * 3600 * 1000
    // Don't overwrite prior verified periods
    const cutoffTime = Math.max(sevenDaysAgo, lastVerTime)

    let tp = 0
    let fp = 0
    let fn = 0
    let tn = 0
    let labeledCount = 0

    // Auto-label events in the 7-day retrospective window
    for (const ev of eventsTable) {
      if (ev.stationId === stationId) {
        const evTime = new Date(ev.timestamp).getTime()
        if (evTime >= cutoffTime && evTime <= nowTime) {
          const isHighOrMed = ev.predictLevel === 'High' || ev.predictLevel === 'Medium'

          if (finding === 'termites_present') {
            if (isHighOrMed) {
              ev.validationOutcome = 'TP'
              tp++
            } else {
              ev.validationOutcome = 'FN'
              fn++
            }
          } else {
            // clean_no_termites
            if (isHighOrMed) {
              ev.validationOutcome = 'FP'
              fp++
            } else {
              ev.validationOutcome = 'TN'
              tn++
            }
          }

          ev.validatedAt = nowIso
          ev.validatedBy = technician
          ev.validationNotes = notes
          labeledCount++
        }
      }
    }

    // Update Station Table record (Notice: threatIndex remains purely mathematical!)
    stationsTable[stIdx] = {
      ...stationsTable[stIdx],
      humanVerificationStatus: finding === 'termites_present'
        ? 'field_verified_infestation'
        : (stationsTable[stIdx].threatLevel === 'High' || stationsTable[stIdx].threatLevel === 'Medium'
            ? 'false_alarm'
            : 'verified_clean'),
      verifiedCleanAt: finding === 'clean_no_termites' ? nowIso : stationsTable[stIdx].verifiedCleanAt,
      lastVerifiedAt: nowIso,
      verifiedBy: technician,
      technicianNotes: notes
    }

    const latest = getLatestEventForStation(stationId)
    const composed = composeStation(stationsTable[stIdx], latest)
    return { labeledCount, tp, fp, fn, tn, station: composed }
  },

  async processAlertAction(
    alertId: string,
    action: 'verify' | 'treat' | 'resolve' | 'mark_false_alarm',
    details: { notes: string; technician?: string; toxicantBatch?: string }
  ): Promise<AlertEvent> {
    await delay(80)
    const alert = alertsTable.find(a => a.id === alertId)
    if (!alert) throw new Error('Alert not found')

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

    if (action === 'verify') {
      alert.status = 'verified_active'
      alert.sopStage = 'treat'
      alert.verificationDate = now
      alert.verifiedBy = details.technician || 'Ju Esen'
      alert.validationOutcome = 'TP'
      alert.technicianNotes = details.notes || 'Technician confirmed active subterranean termite presence.'

      // Auto-label previous 7 days as TP / FN
      this.applyGroundTruthValidation(alert.stationId, 'termites_present', alert.verifiedBy, alert.technicianNotes)
    } else if (action === 'treat') {
      alert.status = 'in_treatment'
      alert.sopStage = 'monitor'
      alert.treatmentDate = now
      alert.toxicantBatch = details.toxicantBatch || 'CHITIN-CSI-B2026-09'
      alert.technicianNotes += `\n[Treated]: ${details.notes || 'In-ground toxicant placed.'}`
    } else if (action === 'resolve') {
      alert.status = 'resolved'
      alert.sopStage = 'closed'
      alert.resolutionDate = now
      alert.technicianNotes += `\n[Resolved]: ${details.notes || 'Sensor returned to baseline; cleared.'}`
      
      // Insert a resolving event into the event table
      await this.insertEvent({
        stationId: alert.stationId,
        timestamp: now,
        eventType: 'daily_heartbeat',
        batteryLevel: 90,
        batteryVoltage: 3.95,
        soilMoisture: 56.0,
        acousticScore: null,
        temperature: 29.0,
        humidity: 78.0,
        rssi: -85,
        snr: 7.5,
        predictLevel: 'Safe',
        aiRiskScore: 12,
        isMoistureTriggered: false
      })
    } else if (action === 'mark_false_alarm') {
      alert.status = 'false_alarm'
      alert.sopStage = 'closed'
      alert.resolutionDate = now
      alert.verifiedBy = details.technician || 'Ju Esen'
      alert.validationOutcome = 'FP'
      alert.probableCause = details.notes || 'Non-biological artifact'
      alert.technicianNotes += `\n[False Alarm]: ${details.notes || 'Non-biological artifact.'}`

      // Auto-label previous 7 days as FP / TN
      this.applyGroundTruthValidation(alert.stationId, 'clean_no_termites', alert.verifiedBy, alert.technicianNotes)

      await this.insertEvent({
        stationId: alert.stationId,
        timestamp: now,
        eventType: 'daily_heartbeat',
        batteryLevel: 90,
        batteryVoltage: 3.95,
        soilMoisture: 56.0,
        acousticScore: null,
        temperature: 29.0,
        humidity: 78.0,
        rssi: -85,
        snr: 7.5,
        predictLevel: 'Safe',
        aiRiskScore: 12,
        isMoistureTriggered: false
      })
    }

    return JSON.parse(JSON.stringify(alert))
  },

  // REPORT FALSE NEGATIVE: Human discovers termites on safe/unflagged station
  async reportFalseNegative(
    stationId: string,
    details: { notes: string; technician?: string; probableCause?: string }
  ): Promise<{ alert: AlertEvent; station: Station; stats: any }> {
    await delay(80)
    const st = stationsTable.find(s => s.id === stationId)
    if (!st) throw new Error(`Station ${stationId} not found`)

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const technician = details.technician || 'Field Technician'
    const notes = details.notes || 'Field inspection confirmed live termite presence unflagged by sensor.'

    // 1. Retroactively label previous 7 days
    const stats = this.applyGroundTruthValidation(stationId, 'termites_present', technician, notes)

    // 2. Inject urgent alert into SOP 'treat' stage (skips verify since human already physically verified it!)
    const alertId = `ALT-FN-${Date.now().toString().slice(-4)}`
    const newAlert: AlertEvent = {
      id: alertId,
      stationId,
      stationName: st.name,
      code: st.code,
      timestamp: now,
      confidenceTier: 'High',
      status: 'verified_active',
      sopStage: 'treat',
      activityScore: st.threatIndex || 60,
      summary: `[FALSE NEGATIVE DETECTED] Human field patrol discovered active Coptotermes subterranean termites on ${st.treeTag} unflagged by automated threshold. Escalated directly to treatment SOP.`,
      technicianNotes: `Reported by ${technician}: ${notes}`,
      verificationDate: now,
      verifiedBy: technician,
      validationOutcome: 'FN',
      probableCause: details.probableCause || 'mudpack_outside_chamber'
    }
    alertsTable.unshift(newAlert)

    return {
      alert: newAlert,
      station: stats.station,
      stats
    }
  },

  // LOG ROUTINE INSPECTION: Routine field patrol confirms clean tree (TN)
  async logRoutineInspection(
    stationId: string,
    details: { notes: string; technician?: string }
  ): Promise<{ station: Station; stats: any }> {
    await delay(60)
    const technician = details.technician || 'Field Technician'
    const notes = details.notes || 'Routine patrol: Clean tree, zero mudpack, healthy bark.'
    const stats = this.applyGroundTruthValidation(stationId, 'clean_no_termites', technician, notes)
    return { station: stats.station, stats }
  },

  // Compute live Ground Truth Confusion Matrix metrics from Event Table
  async getGroundTruthMetrics() {
    await delay(30)
    let tp = 0, fp = 0, fn = 0, tn = 0
    for (const ev of eventsTable) {
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
  },

  // System Metrics
  async getSystemMetrics(): Promise<ForecastModelMetrics> {
    await delay(30)
    return { ...SYSTEM_METRICS }
  },

  async getSystemStatus(): Promise<SystemStatus> {
    await delay(20)
    return { ...systemStatus }
  },

  // =========================================================================
  // TABLE 3 APIs: Verification & Response SOP Cases Table (verification_cases)
  // Dedicated table tracking human verification lifecycle
  // field_verified_infestation -> treatment_start -> monitor -> cleared
  // =========================================================================

  // GET /api/v1/cases
  async getCases(): Promise<VerificationCase[]> {
    await delay(30)
    return JSON.parse(JSON.stringify(casesTable))
  },

  // GET /api/v1/cases/{id}
  async getCaseById(id: string): Promise<VerificationCase | null> {
    await delay(20)
    const found = casesTable.find(c => c.id === id)
    return found ? JSON.parse(JSON.stringify(found)) : null
  },

  // GET /api/v1/cases/station/{stationId}
  async getCasesByStation(stationId: string): Promise<VerificationCase[]> {
    await delay(25)
    return JSON.parse(JSON.stringify(casesTable.filter(c => c.stationId === stationId)))
  },

  // GET /api/v1/cases/active/{stationId}
  async getActiveCaseForStation(stationId: string): Promise<VerificationCase | null> {
    await delay(20)
    const found = casesTable.find(c =>
      c.stationId === stationId &&
      c.status !== 'cleared' &&
      c.status !== 'routine_clean' &&
      c.status !== 'false_alarm' &&
      c.status !== 'terminated'
    )
    return found ? JSON.parse(JSON.stringify(found)) : null
  },

  // POST /api/v1/cases (Generate a new Human Verification & SOP case)
  async createVerificationCase(data: {
    stationId: string
    finding: 'termites_present' | 'clean_no_termites'
    technician: string
    notes: string
    probableCause?: string
  }): Promise<{ caseRecord: VerificationCase; station: Station; stats: any }> {
    await delay(70)
    const stIdx = stationsTable.findIndex(s => s.id === data.stationId)
    if (stIdx === -1) throw new Error(`Station ${data.stationId} not found`)

    const st = stationsTable[stIdx]
    const nowIso = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const caseId = `CASE-2026-${String(casesTable.length + 42).padStart(4, '0')}`

    // 1. Retroactively label preceding 7 days of events
    const stats = this.applyGroundTruthValidation(data.stationId, data.finding, data.technician, data.notes)

    let caseStatus: CaseStatus = 'field_verified_infestation'
    let outcome: 'TP' | 'TN' | 'FP' | 'FN' = 'TP'

    if (data.finding === 'termites_present') {
      caseStatus = 'field_verified_infestation'
      const isHighOrMed = st.threatLevel === 'High' || st.threatLevel === 'Medium'
      outcome = isHighOrMed ? 'TP' : 'FN'

      // Check if an existing alert exists for this station or create an urgent alert
      const existingAlert = alertsTable.find(a => a.stationId === data.stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
      if (existingAlert) {
        existingAlert.status = 'verified_active'
        existingAlert.sopStage = 'treat'
        existingAlert.technicianNotes = data.notes
        existingAlert.verifiedBy = data.technician
        existingAlert.verificationDate = nowIso
      } else {
        alertsTable.unshift({
          id: `ALT-SOP-${Date.now().toString().slice(-4)}`,
          stationId: data.stationId,
          stationName: st.name,
          code: st.code,
          timestamp: nowIso,
          confidenceTier: st.threatLevel,
          status: 'verified_active',
          sopStage: 'treat',
          activityScore: st.threatIndex,
          summary: `[PHYSICAL AUDIT VERIFIED] Field inspection confirmed live Coptotermes infestation on ${st.treeTag}. Response protocol initiated.`,
          technicianNotes: `Reported by ${data.technician}: ${data.notes}`,
          verificationDate: nowIso,
          verifiedBy: data.technician,
          validationOutcome: outcome,
          probableCause: data.probableCause
        })
      }
    } else {
      // Clean finding
      const isHighOrMed = st.threatLevel === 'High' || st.threatLevel === 'Medium'
      caseStatus = isHighOrMed ? 'false_alarm' : 'routine_clean'
      outcome = isHighOrMed ? 'FP' : 'TN'

      // Close any active alerts for this station
      const existingAlert = alertsTable.find(a => a.stationId === data.stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
      if (existingAlert) {
        existingAlert.status = isHighOrMed ? 'false_alarm' : 'resolved'
        existingAlert.sopStage = 'closed'
        existingAlert.resolutionDate = nowIso
        existingAlert.technicianNotes += `\n[Audit Cleared]: ${data.notes}`
      }
    }

    const newCase: VerificationCase = {
      id: caseId,
      stationId: data.stationId,
      stationCode: st.code,
      stationName: st.name,
      createdAt: nowIso,
      status: caseStatus,
      technician: data.technician,
      finding: data.finding,
      probableCause: data.probableCause,
      initialNotes: data.notes,
      verifiedAt: nowIso,
      validationOutcome: outcome,
      ...(caseStatus === 'routine_clean' || caseStatus === 'false_alarm' ? {
        clearedAt: nowIso,
        clearedNotes: data.notes
      } : {})
    }

    casesTable.unshift(newCase)

    const latest = getLatestEventForStation(data.stationId)
    const composed = composeStation(stationsTable[stIdx], latest)

    return {
      caseRecord: JSON.parse(JSON.stringify(newCase)),
      station: composed,
      stats
    }
  },

  // PUT /api/v1/cases/{id}/transition (Advance SOP Lifecycle)
  // field_verified_infestation -> treatment_start -> monitor -> cleared
  async transitionVerificationCase(
    caseId: string,
    nextStatus: CaseStatus,
    details: { technician?: string; notes?: string; toxicantBatch?: string }
  ): Promise<{ caseRecord: VerificationCase; station: Station }> {
    await delay(70)
    const cIdx = casesTable.findIndex(c => c.id === caseId)
    if (cIdx === -1) throw new Error(`Verification case ${caseId} not found`)

    const caseItem = casesTable[cIdx]
    const nowIso = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const stIdx = stationsTable.findIndex(s => s.id === caseItem.stationId)
    if (stIdx === -1) throw new Error(`Station ${caseItem.stationId} not found`)

    caseItem.status = nextStatus

    if (nextStatus === 'treatment_start') {
      caseItem.treatmentStartedAt = nowIso
      caseItem.toxicantBatch = details.toxicantBatch || 'CHITIN-CSI-2026-BATCH-09'
      caseItem.treatmentNotes = details.notes || 'In-ground toxicant placed at bait chamber.'
      stationsTable[stIdx].humanVerificationStatus = 'treatment_start'

      // Also advance corresponding alert if exists
      const alert = alertsTable.find(a => a.stationId === caseItem.stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
      if (alert) {
        alert.sopStage = 'monitor'
        alert.status = 'in_treatment'
        alert.toxicantBatch = caseItem.toxicantBatch
        alert.treatmentDate = nowIso
        alert.technicianNotes += `\n[Treated]: ${caseItem.treatmentNotes}`
      }
    } else if (nextStatus === 'monitor') {
      caseItem.monitoredAt = nowIso
      caseItem.monitoringNotes = details.notes || '40-day colony elimination monitoring commenced.'
      stationsTable[stIdx].humanVerificationStatus = 'monitor'

      const alert = alertsTable.find(a => a.stationId === caseItem.stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
      if (alert) {
        alert.sopStage = 'monitor'
        alert.technicianNotes += `\n[Monitoring]: ${caseItem.monitoringNotes}`
      }
    } else if (nextStatus === 'cleared') {
      caseItem.clearedAt = nowIso
      caseItem.clearedNotes = details.notes || 'Colony fully eradicated; sensor returned to normal baseline.'
      // Only after clear is submitted then only return to normal operation (verified_clean)
      stationsTable[stIdx].humanVerificationStatus = 'verified_clean'
      stationsTable[stIdx].verifiedCleanAt = nowIso

      const alert = alertsTable.find(a => a.stationId === caseItem.stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
      if (alert) {
        alert.status = 'resolved'
        alert.sopStage = 'closed'
        alert.resolutionDate = nowIso
        alert.technicianNotes += `\n[Cleared]: ${caseItem.clearedNotes}`
      }

      // Insert baseline heartbeat event into event table
      await this.insertEvent({
        stationId: caseItem.stationId,
        timestamp: nowIso,
        eventType: 'daily_heartbeat',
        batteryLevel: 92,
        batteryVoltage: 4.0,
        soilMoisture: 56.0,
        acousticScore: null,
        temperature: 29.0,
        humidity: 78.0,
        rssi: -84,
        snr: 7.8,
        predictLevel: 'Safe',
        aiRiskScore: 10,
        isMoistureTriggered: false
      })
    } else if (nextStatus === 'terminated') {
      caseItem.status = 'terminated'
      caseItem.terminatedAt = nowIso
      caseItem.terminatedNotes = details.notes || 'Case terminated early by field technician.'
      // Station returns to normal baseline operation (verified_clean)
      stationsTable[stIdx].humanVerificationStatus = 'verified_clean'
      stationsTable[stIdx].verifiedCleanAt = nowIso
      stationsTable[stIdx].technicianNotes = `[Case Terminated]: ${caseItem.terminatedNotes}`

      const alert = alertsTable.find(a => a.stationId === caseItem.stationId && a.status !== 'resolved' && a.status !== 'false_alarm')
      if (alert) {
        alert.status = 'resolved'
        alert.sopStage = 'closed'
        alert.resolutionDate = nowIso
        alert.technicianNotes += `\n[Case Terminated]: ${caseItem.terminatedNotes}`
      }
    }

    const latest = getLatestEventForStation(caseItem.stationId)
    const composed = composeStation(stationsTable[stIdx], latest)

    return {
      caseRecord: JSON.parse(JSON.stringify(caseItem)),
      station: composed
    }
  },

  // Reset Tables to Default
  resetTables() {
    stationsTable = JSON.parse(JSON.stringify(STATIONS_TABLE))
    eventsTable = createInitialEventsTable()
    alertsTable = JSON.parse(JSON.stringify(INITIAL_ALERTS))
    casesTable = JSON.parse(JSON.stringify(INITIAL_CASES))
  }
}
