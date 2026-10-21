import type { StationInfo, StationEvent, Station, AlertEvent, ForecastModelMetrics, SystemStatus, VerificationCase } from '@/types'

export const MOISTURE_TRIGGER_THRESHOLD = 65.0

// Helper to format days ago for verified clean status
export function formatCleanTimeAgo(timestamp?: string): string {
  if (!timestamp) return 'Verified Clean'
  const time = new Date(timestamp).getTime()
  if (isNaN(time)) return 'Verified Clean'
  const diffMs = Date.now() - time
  const diffDays = Math.floor(diffMs / (24 * 3600 * 1000))
  if (diffDays <= 0) return 'Verified Clean (Today)'
  if (diffDays === 1) return 'Verified Clean (1 day ago)'
  return `Verified Clean (${diffDays} days ago)`
}

// ==========================================
// 1. STATION INFORMATION TABLE (stations)
// Modifiable by user (coordinates, description, type, zone)
// ==========================================
export const STATIONS_TABLE: StationInfo[] = [
  {
    id: 'ST-00',
    code: 'ST-00',
    name: 'Central Hub Station (Master)',
    description: 'Central solar-powered hub station with LoRa P2P concentrator and LoRaWAN uplink to 5G gateway.',
    type: 'master',
    coordinates: [2.58952, 102.09155],
    plantationZone: 'Zone A - Center Core',
    treeTag: 'PW-RMB-0101',
    isProfileComplete: true,
    moistureThreshold: MOISTURE_TRIGGER_THRESHOLD,
    samplingFrequencyMinutes: 15,
    heartbeatIntervalHours: 24,
    isSolarPowered: true,
    installedAt: '2026-08-15 10:00:00',
    threatIndex: 12,
    threatLevel: 'Safe',
    windowHits: 0,
    windowTotal: 6,
    windowSummary: '0/6 triggers - Gated Asleep (Normal Baseline)',
    lastEvaluatedAt: '2026-08-15 10:00:00',
    humanVerificationStatus: 'verified_clean',
    verifiedCleanAt: '2026-10-17 10:00:00',
    lastVerifiedAt: '2026-10-17 10:00:00',
    verifiedBy: 'Ju Esen (Field Technician)'
  },
  {
    id: 'ST-01',
    code: 'ST-01',
    name: 'North Perimeter Station',
    description: 'In-ground bait station monitoring northern perimeter Paulownia row against boundary colonization.',
    type: 'peripheral',
    coordinates: [2.59025, 102.09120],
    plantationZone: 'Zone B - North Row',
    treeTag: 'PW-RMB-0108',
    isProfileComplete: true,
    moistureThreshold: MOISTURE_TRIGGER_THRESHOLD,
    samplingFrequencyMinutes: 15,
    heartbeatIntervalHours: 24,
    isSolarPowered: false,
    installedAt: '2026-08-15 11:30:00',
    threatIndex: 14,
    threatLevel: 'Safe',
    windowHits: 0,
    windowTotal: 6,
    windowSummary: '0/6 triggers - Gated Asleep (Normal Baseline)',
    lastEvaluatedAt: '2026-08-15 11:30:00',
    humanVerificationStatus: 'verified_clean',
    verifiedCleanAt: '2026-10-19 11:30:00',
    lastVerifiedAt: '2026-10-19 11:30:00',
    verifiedBy: 'Ahmad Razak'
  },
  {
    id: 'ST-02',
    code: 'ST-02',
    name: 'East Root-Zone Station',
    description: 'Bait station positioned at high-risk root flare in east buffer zone with active subterranean foraging.',
    type: 'peripheral',
    coordinates: [2.58935, 102.09245],
    plantationZone: 'Zone C - East Buffer',
    treeTag: 'PW-RMB-0115',
    isProfileComplete: true,
    moistureThreshold: MOISTURE_TRIGGER_THRESHOLD,
    samplingFrequencyMinutes: 15,
    heartbeatIntervalHours: 24,
    isSolarPowered: false,
    installedAt: '2026-08-15 14:00:00',
    threatIndex: 72,
    threatLevel: 'Medium',
    windowHits: 4,
    windowTotal: 6,
    windowSummary: '4/6 active detections in past 90 mins (Suspected Active Foraging)',
    lastEvaluatedAt: '2026-10-19 18:45:00',
    humanVerificationStatus: 'unverified'
  },
  {
    id: 'ST-03',
    code: 'ST-03',
    name: 'South-West Boundary Station',
    description: 'Peripheral relay station tracking southern slope Paulownia root system.',
    type: 'peripheral',
    coordinates: [2.58880, 102.09085],
    plantationZone: 'Zone D - South Slope',
    treeTag: 'PW-RMB-0122',
    isProfileComplete: true,
    moistureThreshold: MOISTURE_TRIGGER_THRESHOLD,
    samplingFrequencyMinutes: 15,
    heartbeatIntervalHours: 24,
    isSolarPowered: false,
    installedAt: '2026-08-15 15:45:00',
    threatIndex: 8,
    threatLevel: 'Safe',
    windowHits: 0,
    windowTotal: 6,
    windowSummary: '0/6 triggers - Gated Asleep (Normal Baseline)',
    lastEvaluatedAt: '2026-08-15 15:45:00',
    humanVerificationStatus: 'unverified'
  },
  {
    id: 'ST-04',
    code: 'ST-04',
    name: 'Station ST-04 (Auto-Discovered)',
    description: 'Auto-registered from initial telemetry packet. Awaiting field metadata configuration.',
    type: 'peripheral',
    coordinates: [2.58980, 102.09310],
    plantationZone: '',
    treeTag: '',
    isProfileComplete: false,
    moistureThreshold: MOISTURE_TRIGGER_THRESHOLD,
    samplingFrequencyMinutes: 15,
    heartbeatIntervalHours: 24,
    isSolarPowered: false,
    installedAt: '2026-10-21 20:30:00',
    threatIndex: 54,
    threatLevel: 'Low',
    windowHits: 1,
    windowTotal: 1,
    windowSummary: '1/1 telemetry cycles - Live Telemetry Active',
    lastEvaluatedAt: '2026-10-21 20:30:00',
    humanVerificationStatus: 'unverified'
  }
]

// ==========================================
// 2. EVENT TABLE (events)
// Continually updated by Gateway & Backend API
// Monitored in real-time by WebSocket
// ==========================================
export function createInitialEventsTable(): StationEvent[] {
  const events: StationEvent[] = []
  const now = Date.now()

  // Generate historical events for each station
  STATIONS_TABLE.forEach(st => {
    if (st.id === 'ST-02') {
      // ST-02: Earlier days had daily heartbeats; past 18 hours had 15-minute triggered events
      for (let day = 14; day >= 1; day--) {
        const ts = new Date(now - day * 24 * 3600 * 1000).toISOString().replace('T', ' ').substring(0, 19)
        events.push({
          id: `evt-st02-hb-${day}`,
          stationId: st.id,
          timestamp: ts,
          eventType: 'daily_heartbeat',
          batteryLevel: Math.min(100, 96 - Math.floor(day / 3)),
          batteryVoltage: 4.05,
          soilMoisture: Number((57 + Math.random() * 2.5).toFixed(1)),
          acousticScore: null, // Asleep!
          temperature: 28.5,
          humidity: 80.0,
          rssi: -87,
          snr: 6.4,
          predictLevel: 'Safe',
          aiRiskScore: 14,
          isMoistureTriggered: false
        })
      }

      // Past 18 hours: 15-min triggered packets (72 cycles)
      const cycles = 72
      for (let c = cycles; c >= 0; c--) {
        const ts = new Date(now - c * 15 * 60 * 1000).toISOString().replace('T', ' ').substring(0, 19)
        const hour = (new Date(now - c * 15 * 60 * 1000)).getHours()
        const temp = Number((28 + Math.sin((hour - 8) / 12 * Math.PI) * 3).toFixed(1))
        const hum = Number((84 - Math.sin((hour - 8) / 12 * Math.PI) * 2).toFixed(1))
        const progress = (cycles - c) / cycles
        const act = Math.min(85, Math.floor(45 + progress * 23 + (Math.random() * 6 - 3)))
        const predictLevel = act > 65 ? 'Medium' : 'Low'

        events.push({
          id: `evt-st02-trig-${c}`,
          stationId: st.id,
          timestamp: ts,
          eventType: '15min_triggered_event',
          batteryLevel: 87,
          batteryVoltage: 3.89,
          soilMoisture: Number((68.2 + Math.sin(c / 10) * 1.4 + (Math.random() * 0.4)).toFixed(1)),
          acousticScore: act,
          temperature: temp,
          humidity: hum,
          rssi: -88,
          snr: 6.2,
          predictLevel,
          aiRiskScore: 78,
          isMoistureTriggered: true,
          mfccFeatures: [-9.8, 14.5, 8.2, 6.7, 4.3, 3.1, 2.4, 1.8, 1.2, 0.9, 0.8, 0.4, 0.3]
        })
      }
    } else if (st.id === 'ST-04') {
      // ST-04: Newly Discovered Go-Live Station returning its first telemetry packets
      const ts = new Date(now - 12 * 60 * 1000).toISOString().replace('T', ' ').substring(0, 19)
      events.push({
        id: `evt-st04-first-0`,
        stationId: 'ST-04',
        timestamp: ts,
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
    } else {
      // Untriggered Stations (ST-00, ST-01, ST-03):
      // Only 1 record per day (Daily alive keepalive heartbeat with moisture + battery)
      const baseMoist = st.id === 'ST-00' ? 56 : st.id === 'ST-01' ? 58 : 53
      const baseBat = st.id === 'ST-00' ? 98 : st.id === 'ST-01' ? 92 : 95
      const baseRssi = st.id === 'ST-00' ? -72 : st.id === 'ST-01' ? -84 : -81
      const baseSnr = st.id === 'ST-00' ? 9.4 : st.id === 'ST-01' ? 7.8 : 8.5

      for (let day = 14; day >= 0; day--) {
        const ts = new Date(now - day * 24 * 3600 * 1000).toISOString().replace('T', ' ').substring(0, 19)
        events.push({
          id: `evt-${st.id}-day-${day}`,
          stationId: st.id,
          timestamp: ts,
          eventType: 'daily_heartbeat',
          batteryLevel: Math.max(80, baseBat - Math.floor(day / 4)),
          batteryVoltage: Number((4.10 - (day * 0.01)).toFixed(2)),
          soilMoisture: Number((baseMoist + Math.sin(day) * 1.6).toFixed(1)),
          acousticScore: null, // Gated asleep!
          temperature: 29.5,
          humidity: 78.0,
          rssi: baseRssi,
          snr: baseSnr,
          predictLevel: 'Safe',
          aiRiskScore: 12,
          isMoistureTriggered: false
        })
      }
    }
  })

  // Sort chronologically
  return events.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
}

// Gateway Base Info
export const GATEWAY_INFO = {
  id: 'GW-RMB-5G-01',
  name: 'Rembau Gateway Tower (SX1302 + 5G)',
  coordinates: [2.58985, 102.09180] as [number, number],
  backhaul: '5G' as const,
  signalDbm: -68,
  ip: '10.244.18.2'
}

// Initial Alert
export const INITIAL_ALERTS: AlertEvent[] = [
  {
    id: 'ALT-2026-0041',
    stationId: 'ST-02',
    stationName: 'East Root-Zone Station',
    code: 'ST-02',
    timestamp: '2026-10-19 18:45:00',
    confidenceTier: 'Medium',
    status: 'unverified',
    sopStage: 'verify',
    activityScore: 68,
    summary: 'Moisture threshold breached (69.5% > 65.0%). Piezo circuit awakened; 15-min event table records confirm persistent acoustic foraging clusters. Scheduled verification triggered.',
    technicianNotes: 'Scheduled inspection assigned to on-site forestry technician Ju Esen.'
  }
]

export const SYSTEM_METRICS: ForecastModelMetrics = {
  internalForecastingLeadTimeDays: 14.5,
  detectionLeadTimeDays: 42.0,
  falseAlarmRatePercent: 8.4,
  relayLinkAvailabilityPercent: 98.6,
  masterUplinkAvailabilityPercent: 99.1,
  classificationAccuracyPercent: 94.2,
  averageDailySleepRatioPercent: 99.7
}

export const INITIAL_SYSTEM_STATUS: SystemStatus = {
  gatewayOnline: true,
  gatewayBackhaul: '5G',
  masterConnected: true,
  activeStationsCount: 5,
  totalStationsCount: 5,
  aiInferenceServiceOnline: true,
  lastPacketTime: '8 mins ago',
  timescaleDbHealthy: true
}

// ==========================================
// 3. HUMAN VERIFICATION & RESPONSE SOP CASES TABLE (verification_cases)
// Tracks human audit cases through full treatment & clearance lifecycle
// ==========================================
export const INITIAL_CASES: VerificationCase[] = [
  {
    id: 'CASE-2026-0038',
    stationId: 'ST-00',
    stationCode: 'ST-00',
    stationName: 'Central Hub Station (Master)',
    createdAt: '2026-10-17 10:00:00',
    status: 'cleared',
    technician: 'Ju Esen (Field Technician)',
    finding: 'clean_no_termites',
    initialNotes: 'Routine bi-monthly verification. Station and wood block intact.',
    verifiedAt: '2026-10-17 10:00:00',
    clearedAt: '2026-10-17 10:00:00',
    clearedNotes: 'Station verified completely clean. Routine inspection passed.',
    validationOutcome: 'TN'
  },
  {
    id: 'CASE-2026-0039',
    stationId: 'ST-01',
    stationCode: 'ST-01',
    stationName: 'North Perimeter Station',
    createdAt: '2026-10-19 11:30:00',
    status: 'cleared',
    technician: 'Ahmad Razak',
    finding: 'clean_no_termites',
    initialNotes: 'Routine verification following perimeter soil maintenance.',
    verifiedAt: '2026-10-19 11:30:00',
    clearedAt: '2026-10-19 11:30:00',
    clearedNotes: 'Bait matrix clean with zero insect activity.',
    validationOutcome: 'TN'
  },
  {
    id: 'CASE-2026-0035',
    stationId: 'ST-03',
    stationCode: 'ST-03',
    stationName: 'South-West Boundary Station',
    createdAt: '2026-10-14 09:15:00',
    status: 'terminated',
    technician: 'Ahmad Razak',
    finding: 'termites_present',
    probableCause: 'acoustic_threshold_too_high',
    initialNotes: 'Initial inspection noted minor external soil tunneling near collar flare.',
    verifiedAt: '2026-10-14 09:15:00',
    terminatedAt: '2026-10-16 11:00:00',
    terminatedNotes: 'Colony naturally dispersed following irrigation repair; chamber empty. Case terminated early by technician.',
    validationOutcome: 'TN'
  }
]


// Helper: Compose Station from StationInfo + its Latest Event from Event Table
export function composeStation(info: StationInfo, latestEvent?: StationEvent): Station {
  const isAcousticGated = !latestEvent || latestEvent.acousticScore === null || latestEvent.soilMoisture < info.moistureThreshold

  return {
    ...info,
    zone: info.plantationZone || (info.isProfileComplete === false ? 'Awaiting Zone Setup' : 'Zone A'),
    status: info.threatLevel === 'High' ? 'warning' : 'online',
    latestEvent,
    isProfileComplete: info.isProfileComplete !== false,
    confidenceTier: info.threatLevel || latestEvent?.predictLevel || 'Safe',
    operatingMode: isAcousticGated ? 'dormant_heartbeat' : 'acoustic_triggered',
    isAcousticGated,
    soilMoisture: latestEvent?.soilMoisture ?? 55.0,
    batteryLevel: latestEvent?.batteryLevel ?? 95,
    batteryVoltage: latestEvent?.batteryVoltage ?? 4.0,
    temperature: latestEvent?.temperature ?? 29.5,
    humidity: latestEvent?.humidity ?? 78.0,
    activityScore: latestEvent?.acousticScore ?? null,
    aiRiskScore: info.threatIndex ?? latestEvent?.aiRiskScore ?? 12,
    rssi: latestEvent?.rssi ?? -80,
    snr: latestEvent?.snr ?? 8.0,
    lastSeen: latestEvent ? (
      latestEvent.eventType === '15min_triggered_event'
        ? `${latestEvent.timestamp.split(' ')[1].substring(0, 5)} (15m Event)`
        : `${latestEvent.timestamp.split(' ')[0]} (Daily Heartbeat)`
    ) : 'No events',
    lastHeartbeatTime: latestEvent?.timestamp || info.installedAt,
    lastAcousticCaptureTime: !isAcousticGated ? latestEvent?.timestamp : undefined,
    nextExpectedTransmission: isAcousticGated ? 'Tomorrow 06:00 (Daily Heartbeat)' : 'In 15 mins (15m Window)',
    mfccFeatures: latestEvent?.mfccFeatures
  }
}
