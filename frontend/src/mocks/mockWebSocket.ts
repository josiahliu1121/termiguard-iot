import type { StationEvent, AlertEvent, Station, StationInfo, WebSocketPayload } from '@/types'
import { mockService } from './mockService'
import { MOISTURE_TRIGGER_THRESHOLD } from './mockData'

type WebSocketEventListener = (payload: WebSocketPayload) => void

class MockWebSocketMonitor {
  private listeners: WebSocketEventListener[] = []
  private intervalId: any = null
  private isRunning: boolean = false
  public simTickMs: number = 25000 // In demo mode, automatic 15-min cycle tick check

  connect() {
    if (this.isRunning) return
    this.isRunning = true
    this.intervalId = setInterval(() => this.tick15MinCycle(), this.simTickMs)
  }

  disconnect() {
    this.isRunning = false
    if (this.intervalId) {
      clearInterval(this.intervalId)
      this.intervalId = null
    }
  }

  // Subscribe to Event Table inserts and Station Table updates
  subscribe(callback: WebSocketEventListener) {
    this.listeners.push(callback)
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback)
    }
  }

  private notifyEventInserted(event: StationEvent) {
    this.listeners.forEach(cb => cb({ type: 'event_inserted', event }))
  }

  private notifyStationUpdated(station: Station) {
    this.listeners.forEach(cb => cb({ type: 'station_updated', station }))
  }

  private notifyAlertTriggered(alert: AlertEvent) {
    this.listeners.forEach(cb => cb({ type: 'alert_triggered', alert }))
  }

  // 15-Minute Sampling Window: Queries stations with moisture >= threshold and inserts a new event row
  async tick15MinCycle() {
    const stations = await mockService.getStations()
    const activeStations = stations.filter(s => s.soilMoisture >= s.moistureThreshold)

    if (!activeStations.length) return

    for (const st of activeStations) {
      const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
      const baseAct = st.activityScore || 65
      const actNoise = Math.floor(Math.random() * 7 - 3)
      const currentAct = Math.max(45, Math.min(95, baseAct + actNoise))
      const predictLevel = currentAct > 75 ? 'High' : currentAct > 50 ? 'Medium' : 'Low'

      // INSERT INTO EVENT TABLE
      const newEvent = await mockService.insertEvent({
        stationId: st.id,
        timestamp: now,
        eventType: '15min_triggered_event',
        batteryLevel: st.batteryLevel,
        batteryVoltage: st.batteryVoltage,
        soilMoisture: st.soilMoisture,
        acousticScore: currentAct,
        temperature: st.temperature,
        humidity: st.humidity,
        rssi: st.rssi,
        snr: st.snr,
        predictLevel,
        aiRiskScore: Math.min(98, currentAct + 8),
        isMoistureTriggered: true,
        mfccFeatures: [-9.8, 14.5, 8.2, 6.7, 4.3, 3.1, 2.4, 1.8, 1.2, 0.9, 0.8, 0.4, 0.3]
      })

      // WEBSOCKET BROADCAST TO CLIENTS (Event Table insert)
      this.notifyEventInserted(newEvent)

      // EVALUATE TEMPORAL PERSISTENCE & THREAT INDEX -> UPDATE STATION TABLE
      const updatedStation = mockService.evaluateAndUpdateStationThreat(st.id)

      // WEBSOCKET BROADCAST TO CLIENTS (Station Table update)
      this.notifyStationUpdated(updatedStation)
    }
  }

  // Manually Trigger 15-Minute Poll
  async advance15MinCycle() {
    await this.tick15MinCycle()
  }

  // Insert a 24-Hour Daily Keepalive Heartbeat Event for a station
  async emitDailyHeartbeat(stationId: string = 'ST-00') {
    const st = await mockService.getStationById(stationId)
    if (!st) return

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

    // INSERT INTO EVENT TABLE
    const newEvent = await mockService.insertEvent({
      stationId,
      timestamp: now,
      eventType: 'daily_heartbeat',
      batteryLevel: st.batteryLevel,
      batteryVoltage: st.batteryVoltage,
      soilMoisture: st.soilMoisture,
      acousticScore: null, // Gated asleep!
      temperature: st.temperature,
      humidity: st.humidity,
      rssi: st.rssi,
      snr: st.snr,
      predictLevel: 'Safe',
      aiRiskScore: 12,
      isMoistureTriggered: false
    })

    // WEBSOCKET BROADCAST
    this.notifyEventInserted(newEvent)

    // Evaluate temporal health & update station table
    const updatedStation = mockService.evaluateAndUpdateStationThreat(stationId)
    this.notifyStationUpdated(updatedStation)
  }

  // Simulate Soil Moisture Breach -> Inserts triggered event into Event Table & alerts
  async triggerMoistureBreach(stationId: string = 'ST-02') {
    const st = await mockService.getStationById(stationId)
    if (!st) return

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

    // INSERT TRIGGERED ROW INTO EVENT TABLE
    const newEvent = await mockService.insertEvent({
      stationId,
      timestamp: now,
      eventType: '15min_triggered_event',
      batteryLevel: st.batteryLevel,
      batteryVoltage: st.batteryVoltage,
      soilMoisture: 72.8, // > 65% trigger
      acousticScore: 88, // awakened!
      temperature: st.temperature,
      humidity: st.humidity,
      rssi: st.rssi,
      snr: st.snr,
      predictLevel: 'High',
      aiRiskScore: 95,
      isMoistureTriggered: true,
      mfccFeatures: [-6.2, 19.4, 12.1, 9.8, 7.3, 5.2, 4.0, 3.1, 2.5, 1.9, 1.2, 0.9, 0.6]
    })

    // WEBSOCKET BROADCAST (Event Table)
    this.notifyEventInserted(newEvent)

    // EVALUATE TEMPORAL PERSISTENCE & THREAT INDEX -> UPDATE STATION TABLE
    const updatedStation = mockService.evaluateAndUpdateStationThreat(stationId)

    // WEBSOCKET BROADCAST (Station Table)
    this.notifyStationUpdated(updatedStation)

    const newAlert: AlertEvent = {
      id: `ALT-${Date.now().toString().slice(-4)}`,
      stationId,
      stationName: st.name,
      code: st.code,
      timestamp: now,
      confidenceTier: updatedStation.threatLevel,
      status: 'unverified',
      sopStage: 'verify',
      activityScore: 88,
      summary: `[${updatedStation.threatLevel.toUpperCase()} THREAT] Temporal window evaluation: ${updatedStation.windowSummary}. Rolling ITI Score: ${updatedStation.threatIndex}/100. Verification protocol engaged.`,
      technicianNotes: 'Expedited verification required.'
    }

    const alerts = await mockService.getAlerts()
    alerts.unshift(newAlert)

    // WEBSOCKET BROADCAST (Alerts)
    this.notifyAlertTriggered(newAlert)
  }

  // Reset Station to Dry Soil / Asleep
  async resetToBaseline(stationId: string = 'ST-02') {
    const st = await mockService.getStationById(stationId)
    if (!st) return

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

    // INSERT DORMANT EVENT INTO EVENT TABLE
    const newEvent = await mockService.insertEvent({
      stationId,
      timestamp: now,
      eventType: 'daily_heartbeat',
      batteryLevel: st.batteryLevel,
      batteryVoltage: st.batteryVoltage,
      soilMoisture: 56.4,
      acousticScore: null,
      temperature: 28.4,
      humidity: 79.0,
      rssi: st.rssi,
      snr: st.snr,
      predictLevel: 'Safe',
      aiRiskScore: 10,
      isMoistureTriggered: false
    })

    this.notifyEventInserted(newEvent)

    const updatedStation = mockService.evaluateAndUpdateStationThreat(stationId)
    this.notifyStationUpdated(updatedStation)
  }

  // Simulate newly deployed station broadcasting its first telemetry packet
  async simulateNewStationDiscovery(stationId: string = 'ST-05') {
    const existing = await mockService.getStationById(stationId)
    if (existing) return existing

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const newStationInfo: Partial<StationInfo> = {
      id: stationId,
      code: stationId,
      name: `Station ${stationId} (Auto-Discovered)`,
      description: 'Auto-registered from initial telemetry packet. Awaiting field metadata configuration.',
      type: 'peripheral',
      coordinates: [2.58910, 102.09340],
      plantationZone: 'Unassigned (Pending Setup)',
      treeTag: '',
      isProfileComplete: false,
      moistureThreshold: 65.0,
      samplingFrequencyMinutes: 15,
      heartbeatIntervalHours: 24,
      threatIndex: 45,
      threatLevel: 'Low',
      windowHits: 1,
      windowTotal: 1,
      windowSummary: '1/1 telemetry cycles - Live Telemetry Active',
      installedAt: now,
      humanVerificationStatus: 'unverified'
    }

    const createdStation = await mockService.addStationInfo(newStationInfo)

    // Insert first telemetry event
    const firstEvent = await mockService.insertEvent({
      stationId,
      timestamp: now,
      eventType: 'daily_heartbeat',
      batteryLevel: 99,
      batteryVoltage: 4.18,
      soilMoisture: 66.8,
      acousticScore: 48,
      temperature: 28.8,
      humidity: 82.5,
      rssi: -78,
      snr: 8.9,
      predictLevel: 'Low',
      aiRiskScore: 45,
      isMoistureTriggered: true,
      mfccFeatures: [-8.0, 10.5, 5.8, 4.2, 2.9, 1.8, 1.3, 0.9, 0.7, 0.4, 0.3, 0.2, 0.1]
    })

    this.notifyEventInserted(firstEvent)
    this.notifyStationUpdated(createdStation)
    return createdStation
  }
}

export const mockWebSocket = new MockWebSocketMonitor()
