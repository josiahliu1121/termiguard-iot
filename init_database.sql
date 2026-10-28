-- ============================================================================
-- TERMA-GUARD.AI: Subterranean Termite Early-Warning IoT System
-- Database Initialization Script for MySQL 8.0+
-- Database: termiguard_db
-- ============================================================================

CREATE DATABASE IF NOT EXISTS termiguard_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE termiguard_db;

-- Drop tables in reverse order of foreign key dependencies
DROP TABLE IF EXISTS alerts;
DROP TABLE IF EXISTS verification_cases;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS stations;
DROP TABLE IF EXISTS gateway_info;

-- ============================================================================
-- 1. TABLE: stations (Station Information Table)
-- Stores static hardware metadata, spatial GPS, tree assignment, operating
-- thresholds, and rolling multi-event Threat Index (ITI).
-- ============================================================================
CREATE TABLE stations (
  id VARCHAR(32) PRIMARY KEY,                         -- Unique Station ID (e.g. 'ST-00', 'ST-01')
  code VARCHAR(32) NOT NULL,                          -- Visual code identifier (matches id)
  name VARCHAR(128) NOT NULL,                         -- Descriptive station name
  description TEXT,                                   -- Field notes & placement description
  type ENUM('master', 'peripheral') NOT NULL DEFAULT 'peripheral',
  latitude DECIMAL(10, 7) NOT NULL,                   -- GPS Latitude (e.g. 2.5895200)
  longitude DECIMAL(10, 7) NOT NULL,                  -- GPS Longitude (e.g. 102.0915500)
  plantation_zone VARCHAR(64) NOT NULL DEFAULT '',    -- Forestry zone (e.g. 'Zone A - Center Core')
  tree_tag VARCHAR(64) NOT NULL DEFAULT '',           -- Paulownia tree tag ID (e.g. 'PW-RMB-0101')
  is_profile_complete BOOLEAN NOT NULL DEFAULT TRUE,  -- FALSE for auto-registered nodes awaiting user setup
  moisture_threshold DECIMAL(5, 2) NOT NULL DEFAULT 65.00, -- Awakens acoustic circuit at >= 65%
  sampling_frequency_minutes INT NOT NULL DEFAULT 15, -- 15-min sampling cadence when active
  heartbeat_interval_hours INT NOT NULL DEFAULT 24,   -- 24-hr daily sleep keepalive interval
  is_solar_powered BOOLEAN NOT NULL DEFAULT FALSE,    -- Solar hub (true) vs LiSOCl2 battery (false)
  installed_at DATETIME NOT NULL,                     -- Date of deployment
  
  -- Rolling Window & Continuous Threat Metrics (ITI: 0-100)
  threat_index INT NOT NULL DEFAULT 10,               -- Continuous Infestation Threat Index (0 - 100)
  threat_level ENUM('Safe', 'Low', 'Medium', 'High') NOT NULL DEFAULT 'Safe',
  window_hits INT NOT NULL DEFAULT 0,                 -- Active triggers in rolling window (M of N)
  window_total INT NOT NULL DEFAULT 6,                -- Total cycles evaluated in sliding window (N=6)
  window_summary VARCHAR(255) NOT NULL DEFAULT '',    -- E.g. "4/6 active detections in past 90 mins"
  last_evaluated_at DATETIME DEFAULT NULL,            -- Timestamp of last temporal evaluation
  
  -- Human Ground-Truth & Response SOP Lifecycle
  human_verification_status ENUM(
    'unverified',
    'field_verified_infestation',
    'treatment_start',
    'monitor',
    'verified_clean',
    'false_alarm',
    'terminated'
  ) NOT NULL DEFAULT 'unverified',
  last_verified_at DATETIME DEFAULT NULL,
  verified_clean_at DATETIME DEFAULT NULL,
  verified_by VARCHAR(128) DEFAULT NULL,
  technician_notes TEXT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================================
-- 2. TABLE: events (Time-Series Telemetry Table)
-- High-throughput append-only table recording 15-min acoustic sampling packets
-- and 24-hr daily keepalive heartbeats forwarded by the LoRaWAN Concentrator.
-- ============================================================================
CREATE TABLE events (
  id VARCHAR(64) PRIMARY KEY,                         -- Unique event identifier (e.g. 'evt-1718000000')
  station_id VARCHAR(32) NOT NULL,                    -- References stations.id
  timestamp DATETIME NOT NULL,                        -- Time of sensing / LoRa uplink
  event_type ENUM(
    'daily_heartbeat',
    '15min_triggered_event'
  ) NOT NULL,
  battery_level INT NOT NULL,                         -- 0 - 100%
  battery_voltage DECIMAL(4, 2) NOT NULL,             -- 3.60V - 4.20V
  soil_moisture DECIMAL(5, 2) NOT NULL,               -- Capacitive probe reading (0 - 100%)
  acoustic_score INT DEFAULT NULL,                    -- Piezo activity score (NULL when gated asleep)
  temperature DECIMAL(4, 1) NOT NULL,                 -- Microclimate ambient temperature (°C)
  humidity DECIMAL(4, 1) NOT NULL,                    -- Microclimate relative humidity (%)
  rssi INT NOT NULL,                                  -- LoRa link signal strength (dBm)
  snr DECIMAL(4, 1) NOT NULL,                         -- Signal-to-Noise Ratio (dB)
  predict_level ENUM('Safe', 'Low', 'Medium', 'High') NOT NULL DEFAULT 'Safe',
  ai_risk_score INT NOT NULL DEFAULT 10,              -- Escalation probability score (0 - 100%)
  is_moisture_triggered BOOLEAN NOT NULL DEFAULT FALSE,
  mfcc_features JSON DEFAULT NULL,                    -- 13-band Mel-Frequency Cepstral Coefficients
  
  -- Retroactive Ground-Truth Validation Outcomes
  validation_outcome ENUM('TP', 'TN', 'FP', 'FN') DEFAULT NULL,
  validated_at DATETIME DEFAULT NULL,
  validated_by VARCHAR(128) DEFAULT NULL,
  validation_notes TEXT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_events_station FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE,
  INDEX idx_station_time (station_id, timestamp),
  INDEX idx_event_type (event_type),
  INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB;

-- ============================================================================
-- 3. TABLE: verification_cases (Human Verification & Response SOP Cases)
-- Tracks the lifecycle of physical field ground truth audits and toxicant baiting
-- response protocols (Verify -> Treat -> Monitor -> Cleared / Terminated).
-- ============================================================================
CREATE TABLE verification_cases (
  id VARCHAR(64) PRIMARY KEY,                         -- E.g. 'CASE-2026-0038'
  station_id VARCHAR(32) NOT NULL,                    -- References stations.id
  station_code VARCHAR(32) NOT NULL,
  station_name VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL,
  status ENUM(
    'field_verified_infestation',
    'treatment_start',
    'monitor',
    'cleared',
    'false_alarm',
    'routine_clean',
    'terminated'
  ) NOT NULL,
  technician VARCHAR(128) NOT NULL,
  finding ENUM('termites_present', 'clean_no_termites') NOT NULL,
  probable_cause VARCHAR(128) DEFAULT NULL,
  initial_notes TEXT NOT NULL,
  
  -- Audit Trail Timestamps & Notes
  verified_at DATETIME DEFAULT NULL,
  treatment_started_at DATETIME DEFAULT NULL,
  toxicant_batch VARCHAR(64) DEFAULT NULL,            -- Chemical batch ID (e.g. 'CHITIN-CSI-2026-09')
  treatment_notes TEXT DEFAULT NULL,
  monitored_at DATETIME DEFAULT NULL,
  monitoring_notes TEXT DEFAULT NULL,
  cleared_at DATETIME DEFAULT NULL,
  cleared_notes TEXT DEFAULT NULL,
  terminated_at DATETIME DEFAULT NULL,
  terminated_notes TEXT DEFAULT NULL,
  validation_outcome ENUM('TP', 'TN', 'FP', 'FN') DEFAULT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_cases_station FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE,
  INDEX idx_cases_station (station_id),
  INDEX idx_cases_status (status)
) ENGINE=InnoDB;

-- ============================================================================
-- 4. TABLE: alerts (Pest Early-Warning Incident Alerts)
-- Stores automated alerts generated when moisture and acoustics breach thresholds.
-- ============================================================================
CREATE TABLE alerts (
  id VARCHAR(64) PRIMARY KEY,                         -- E.g. 'ALT-2026-0041'
  station_id VARCHAR(32) NOT NULL,
  station_name VARCHAR(128) NOT NULL,
  code VARCHAR(32) NOT NULL,
  timestamp DATETIME NOT NULL,
  confidence_tier ENUM('Safe', 'Low', 'Medium', 'High') NOT NULL,
  status ENUM(
    'unverified',
    'verified_active',
    'in_treatment',
    'resolved',
    'false_alarm'
  ) NOT NULL DEFAULT 'unverified',
  sop_stage ENUM('verify', 'treat', 'monitor', 'closed') NOT NULL DEFAULT 'verify',
  summary TEXT NOT NULL,
  activity_score INT NOT NULL,
  technician_notes TEXT DEFAULT NULL,
  verification_date DATETIME DEFAULT NULL,
  treatment_date DATETIME DEFAULT NULL,
  toxicant_batch VARCHAR(64) DEFAULT NULL,
  resolution_date DATETIME DEFAULT NULL,
  validation_outcome ENUM('TP', 'FP', 'FN') DEFAULT NULL,
  probable_cause VARCHAR(128) DEFAULT NULL,
  verified_by VARCHAR(128) DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_alerts_station FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE,
  INDEX idx_alerts_station (station_id),
  INDEX idx_alerts_status (status)
) ENGINE=InnoDB;

-- ============================================================================
-- 5. TABLE: gateway_info (Concentrator Gateway Tower Status)
-- ============================================================================
CREATE TABLE gateway_info (
  id VARCHAR(64) PRIMARY KEY,                         -- E.g. 'GW-RMB-5G-01'
  name VARCHAR(128) NOT NULL,
  latitude DECIMAL(10, 7) NOT NULL,
  longitude DECIMAL(10, 7) NOT NULL,
  backhaul VARCHAR(32) NOT NULL DEFAULT '5G',
  signal_dbm INT NOT NULL DEFAULT -68,
  ip_address VARCHAR(64) NOT NULL DEFAULT '10.244.18.2',
  is_online BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================================
-- SEED DATA INSERTION
-- Populates the exact baseline state matching the frontend application
-- ============================================================================

-- Seed 1: Gateway Tower
INSERT INTO gateway_info (id, name, latitude, longitude, backhaul, signal_dbm, ip_address, is_online)
VALUES ('GW-RMB-5G-01', 'Rembau Gateway Tower (SX1302 + 5G)', 2.5898500, 102.0918000, '5G', -68, '10.244.18.2', TRUE);

-- Seed 2: Five Initial Plantation Bait Stations
INSERT INTO stations (
  id, code, name, description, type, latitude, longitude,
  plantation_zone, tree_tag, is_profile_complete, moisture_threshold,
  sampling_frequency_minutes, heartbeat_interval_hours, is_solar_powered,
  installed_at, threat_index, threat_level, window_hits, window_total,
  window_summary, last_evaluated_at, human_verification_status,
  last_verified_at, verified_clean_at, verified_by, technician_notes
) VALUES
(
  'ST-00', 'ST-00', 'Central Hub Station (Master)',
  'Central solar-powered hub station with LoRa P2P concentrator and LoRaWAN uplink to 5G gateway.',
  'master', 2.5895200, 102.0915500, 'Zone A - Center Core', 'PW-RMB-0101',
  TRUE, 65.00, 15, 24, TRUE, '2026-08-15 10:00:00',
  12, 'Safe', 0, 6, '0/6 triggers - Gated Asleep (Normal Baseline)',
  '2026-08-15 10:00:00', 'verified_clean', '2026-10-17 10:00:00', '2026-10-17 10:00:00',
  'Ju Esen (Field Technician)', NULL
),
(
  'ST-01', 'ST-01', 'North Perimeter Station',
  'In-ground bait station monitoring northern perimeter Paulownia row against boundary colonization.',
  'peripheral', 2.5902500, 102.0912000, 'Zone A - Center Core', 'PW-RMB-0108',
  TRUE, 65.00, 15, 24, FALSE, '2026-08-15 11:30:00',
  14, 'Safe', 0, 6, '0/6 triggers - Gated Asleep (Normal Baseline)',
  '2026-08-15 11:30:00', 'verified_clean', '2026-10-19 11:30:00', '2026-10-19 11:30:00',
  'Ahmad Razak', NULL
),
(
  'ST-02', 'ST-02', 'East Root-Zone Station',
  'Bait station positioned at high-risk root flare in east buffer zone with active subterranean foraging.',
  'peripheral', 2.5893500, 102.0924500, 'Zone A - Center Core', 'PW-RMB-0115',
  TRUE, 65.00, 15, 24, FALSE, '2026-08-15 14:00:00',
  72, 'Medium', 4, 6, '4/6 active detections in past 90 mins (Suspected Active Foraging)',
  '2026-10-19 18:45:00', 'unverified', NULL, NULL, NULL, NULL
),
(
  'ST-03', 'ST-03', 'South-West Boundary Station',
  'Peripheral relay station tracking southern slope Paulownia root system.',
  'peripheral', 2.5888000, 102.0908500, 'Zone A - Center Core', 'PW-RMB-0122',
  TRUE, 65.00, 15, 24, FALSE, '2026-08-15 15:45:00',
  8, 'Safe', 0, 6, '0/6 triggers - Gated Asleep (Normal Baseline)',
  '2026-08-15 15:45:00', 'unverified', NULL, NULL, NULL, NULL
),
(
  'ST-04', 'ST-04', 'Station ST-04 (Auto-Discovered)',
  'Auto-registered from initial telemetry packet. Awaiting field metadata configuration.',
  'peripheral', 2.5898000, 102.0931000, 'Unassigned (Pending Setup)', '',
  FALSE, 65.00, 15, 24, FALSE, '2026-10-21 20:30:00',
  54, 'Low', 1, 1, '1/1 telemetry cycles - Live Telemetry Active',
  '2026-10-21 20:30:00', 'unverified', NULL, NULL, NULL, NULL
);

-- Seed 4: Historical Ground-Truth Verification Cases
INSERT INTO verification_cases (
  id, station_id, station_code, station_name, created_at, status,
  technician, finding, probable_cause, initial_notes, verified_at,
  treatment_started_at, toxicant_batch, treatment_notes, monitored_at,
  monitoring_notes, cleared_at, cleared_notes, terminated_at, terminated_notes,
  validation_outcome
) VALUES
(
  'CASE-2026-0038', 'ST-00', 'ST-00', 'Central Hub Station (Master)',
  '2026-10-17 10:00:00', 'cleared', 'Ju Esen (Field Technician)',
  'clean_no_termites', NULL, 'Routine bi-monthly verification. Station and wood block intact.',
  '2026-10-17 10:00:00', NULL, NULL, NULL, NULL, NULL,
  '2026-10-17 10:00:00', 'Station verified completely clean. Routine inspection passed.', NULL, NULL, 'TN'
),
(
  'CASE-2026-0039', 'ST-01', 'ST-01', 'North Perimeter Station',
  '2026-10-19 11:30:00', 'cleared', 'Ahmad Razak',
  'clean_no_termites', NULL, 'Routine verification following perimeter soil maintenance.',
  '2026-10-19 11:30:00', NULL, NULL, NULL, NULL, NULL,
  '2026-10-19 11:30:00', 'Bait matrix clean with zero insect activity.', NULL, NULL, 'TN'
),
(
  'CASE-2026-0035', 'ST-03', 'ST-03', 'South-West Boundary Station',
  '2026-10-14 09:15:00', 'terminated', 'Ahmad Razak',
  'termites_present', 'acoustic_threshold_too_high',
  'Initial inspection noted minor external soil tunneling near collar flare.',
  '2026-10-14 09:15:00', NULL, NULL, NULL, NULL, NULL,
  NULL, NULL, '2026-10-16 11:00:00',
  'Colony naturally dispersed following irrigation repair; chamber empty. Case terminated early by technician.',
  'TN'
);

-- Seed 5: Active Incident Alert
INSERT INTO alerts (
  id, station_id, station_name, code, timestamp, confidence_tier,
  status, sop_stage, summary, activity_score, technician_notes
) VALUES
(
  'ALT-2026-0041', 'ST-02', 'East Root-Zone Station', 'ST-02',
  '2026-10-19 18:45:00', 'Medium', 'unverified', 'verify',
  'Moisture threshold breached (69.5% > 65.0%). Piezo circuit awakened; 15-min event table records confirm persistent acoustic foraging clusters. Scheduled verification triggered.',
  68, 'Scheduled inspection assigned to on-site forestry technician Ju Esen.'
);

-- Seed 6: Telemetry Event Records
-- Uses dynamic relative timestamps (DATE_SUB) so records are always fresh within the 7-day retrospective window
-- All initial events start UNVALIDATED (validation_outcome = NULL) awaiting human ground-truth audits
INSERT INTO events (
  id, station_id, timestamp, event_type, battery_level, battery_voltage,
  soil_moisture, acoustic_score, temperature, humidity, rssi, snr,
  predict_level, ai_risk_score, is_moisture_triggered, mfcc_features,
  validation_outcome, validated_at, validated_by, validation_notes
) VALUES
-- 1. ST-00 (Central Hub - Safe baseline keepalives)
('evt-st00-day-6', 'ST-00', DATE_SUB(NOW(), INTERVAL 6 DAY), 'daily_heartbeat', 98, 4.15, 55.8, NULL, 29.2, 77.5, -72, 9.4, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st00-day-5', 'ST-00', DATE_SUB(NOW(), INTERVAL 5 DAY), 'daily_heartbeat', 98, 4.14, 56.1, NULL, 29.5, 78.0, -72, 9.3, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st00-day-4', 'ST-00', DATE_SUB(NOW(), INTERVAL 4 DAY), 'daily_heartbeat', 97, 4.13, 56.4, NULL, 29.0, 78.2, -73, 9.2, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st00-day-3', 'ST-00', DATE_SUB(NOW(), INTERVAL 3 DAY), 'daily_heartbeat', 97, 4.12, 56.0, NULL, 29.6, 77.8, -72, 9.5, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st00-day-2', 'ST-00', DATE_SUB(NOW(), INTERVAL 2 DAY), 'daily_heartbeat', 96, 4.11, 56.5, NULL, 29.4, 78.5, -72, 9.3, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st00-day-1', 'ST-00', DATE_SUB(NOW(), INTERVAL 1 DAY), 'daily_heartbeat', 96, 4.10, 56.2, NULL, 29.3, 78.0, -71, 9.6, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st00-today', 'ST-00', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'daily_heartbeat', 95, 4.09, 56.3, NULL, 29.5, 77.9, -72, 9.4, 'Safe', 10, FALSE, NULL, NULL, NULL, NULL, NULL),

-- 2. ST-01 (North Perimeter - Safe baseline keepalives)
('evt-st01-day-6', 'ST-01', DATE_SUB(NOW(), INTERVAL 6 DAY), 'daily_heartbeat', 93, 4.04, 57.8, NULL, 28.9, 79.0, -84, 7.8, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st01-day-5', 'ST-01', DATE_SUB(NOW(), INTERVAL 5 DAY), 'daily_heartbeat', 93, 4.03, 58.2, NULL, 29.1, 79.4, -84, 7.7, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st01-day-4', 'ST-01', DATE_SUB(NOW(), INTERVAL 4 DAY), 'daily_heartbeat', 92, 4.02, 58.0, NULL, 29.0, 79.2, -85, 7.9, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st01-day-3', 'ST-01', DATE_SUB(NOW(), INTERVAL 3 DAY), 'daily_heartbeat', 92, 4.01, 58.5, NULL, 29.3, 79.8, -84, 7.6, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st01-day-2', 'ST-01', DATE_SUB(NOW(), INTERVAL 2 DAY), 'daily_heartbeat', 91, 4.00, 58.1, NULL, 29.0, 79.5, -83, 7.8, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st01-day-1', 'ST-01', DATE_SUB(NOW(), INTERVAL 1 DAY), 'daily_heartbeat', 91, 3.99, 58.4, NULL, 28.8, 80.0, -84, 7.7, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st01-today', 'ST-01', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'daily_heartbeat', 90, 3.98, 58.1, NULL, 29.0, 79.5, -84, 7.8, 'Safe', 12, FALSE, NULL, NULL, NULL, NULL, NULL),

-- 3. ST-03 (South-West Boundary - Safe baseline keepalives)
('evt-st03-day-6', 'ST-03', DATE_SUB(NOW(), INTERVAL 6 DAY), 'daily_heartbeat', 96, 4.10, 53.0, NULL, 29.0, 76.8, -81, 8.5, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st03-day-5', 'ST-03', DATE_SUB(NOW(), INTERVAL 5 DAY), 'daily_heartbeat', 95, 4.09, 53.2, NULL, 29.2, 77.0, -81, 8.4, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st03-day-4', 'ST-03', DATE_SUB(NOW(), INTERVAL 4 DAY), 'daily_heartbeat', 95, 4.08, 53.5, NULL, 29.1, 77.2, -82, 8.6, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st03-day-3', 'ST-03', DATE_SUB(NOW(), INTERVAL 3 DAY), 'daily_heartbeat', 94, 4.07, 53.1, NULL, 29.4, 76.9, -81, 8.3, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st03-day-2', 'ST-03', DATE_SUB(NOW(), INTERVAL 2 DAY), 'daily_heartbeat', 94, 4.06, 53.6, NULL, 29.0, 77.5, -80, 8.5, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st03-day-1', 'ST-03', DATE_SUB(NOW(), INTERVAL 1 DAY), 'daily_heartbeat', 93, 4.05, 53.3, NULL, 29.3, 77.1, -81, 8.6, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st03-today', 'ST-03', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'daily_heartbeat', 93, 4.04, 53.4, NULL, 29.2, 77.0, -81, 8.5, 'Safe', 8, FALSE, NULL, NULL, NULL, NULL, NULL),

-- 4. ST-04 (Auto-Discovered Station - Recent Telemetry stream)
('evt-st04-pkt-2', 'ST-04', DATE_SUB(NOW(), INTERVAL 45 MINUTE), 'daily_heartbeat', 98, 4.14, 67.2, 52, 29.1, 80.8, -79, 8.3, 'Low', 45, TRUE, '[-8.5, 11.2, 6.4, 4.8, 3.1, 2.0, 1.5, 1.1, 0.8, 0.5, 0.4, 0.2, 0.1]', NULL, NULL, NULL, NULL),
('evt-st04-pkt-1', 'ST-04', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'daily_heartbeat', 98, 4.14, 67.3, 53, 29.2, 81.0, -79, 8.4, 'Low', 47, TRUE, '[-8.5, 11.2, 6.4, 4.8, 3.1, 2.0, 1.5, 1.1, 0.8, 0.5, 0.4, 0.2, 0.1]', NULL, NULL, NULL, NULL),
('evt-st04-first-0', 'ST-04', DATE_SUB(NOW(), INTERVAL 15 MINUTE), 'daily_heartbeat', 98, 4.14, 67.4, 54, 29.2, 81.0, -79, 8.4, 'Low', 48, TRUE, '[-8.5, 11.2, 6.4, 4.8, 3.1, 2.0, 1.5, 1.1, 0.8, 0.5, 0.4, 0.2, 0.1]', NULL, NULL, NULL, NULL),

-- 5. ST-02 (East Root-Zone Hotspot - Active subterranean foraging acoustic bursts)
('evt-st02-day-4', 'ST-02', DATE_SUB(NOW(), INTERVAL 4 DAY), 'daily_heartbeat', 91, 3.98, 59.2, NULL, 29.4, 80.0, -87, 6.8, 'Safe', 14, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st02-day-3', 'ST-02', DATE_SUB(NOW(), INTERVAL 3 DAY), 'daily_heartbeat', 90, 3.96, 61.5, NULL, 29.6, 81.0, -87, 6.7, 'Safe', 18, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st02-day-2', 'ST-02', DATE_SUB(NOW(), INTERVAL 2 DAY), 'daily_heartbeat', 89, 3.95, 64.8, NULL, 29.5, 82.0, -88, 6.5, 'Low', 28, FALSE, NULL, NULL, NULL, NULL, NULL),
('evt-st02-day-1', 'ST-02', DATE_SUB(NOW(), INTERVAL 1 DAY), 'daily_heartbeat', 88, 3.93, 67.2, 58, 29.7, 82.5, -88, 6.5, 'Medium', 65, TRUE, '[-9.2, 13.8, 7.8, 6.2, 4.0, 2.8, 2.1, 1.5, 1.0, 0.8, 0.6, 0.3, 0.2]', NULL, NULL, NULL, NULL),
('evt-st02-trig-05', 'ST-02', DATE_SUB(NOW(), INTERVAL 90 MINUTE), '15min_triggered_event', 88, 3.92, 69.1, 68, 29.8, 83.0, -88, 6.4, 'Medium', 72, TRUE, '[-9.5, 14.2, 8.0, 6.5, 4.2, 3.0, 2.3, 1.7, 1.1, 0.9, 0.7, 0.4, 0.3]', NULL, NULL, NULL, NULL),
('evt-st02-trig-04', 'ST-02', DATE_SUB(NOW(), INTERVAL 75 MINUTE), '15min_triggered_event', 87, 3.91, 69.2, 74, 29.7, 83.2, -88, 6.3, 'Medium', 76, TRUE, '[-9.6, 14.3, 8.1, 6.6, 4.2, 3.0, 2.3, 1.7, 1.1, 0.9, 0.7, 0.4, 0.3]', NULL, NULL, NULL, NULL),
('evt-st02-trig-03', 'ST-02', DATE_SUB(NOW(), INTERVAL 60 MINUTE), '15min_triggered_event', 87, 3.90, 69.4, 78, 29.6, 83.5, -88, 6.1, 'High', 82, TRUE, '[-9.8, 14.5, 8.2, 6.7, 4.3, 3.1, 2.4, 1.8, 1.2, 0.9, 0.8, 0.4, 0.3]', NULL, NULL, NULL, NULL),
('evt-st02-trig-02', 'ST-02', DATE_SUB(NOW(), INTERVAL 45 MINUTE), '15min_triggered_event', 87, 3.89, 69.6, 80, 29.5, 83.8, -89, 6.0, 'High', 84, TRUE, '[-9.9, 14.6, 8.3, 6.8, 4.4, 3.2, 2.4, 1.8, 1.2, 0.9, 0.8, 0.4, 0.3]', NULL, NULL, NULL, NULL),
('evt-st02-trig-01', 'ST-02', DATE_SUB(NOW(), INTERVAL 30 MINUTE), '15min_triggered_event', 86, 3.89, 69.8, 84, 29.4, 84.0, -89, 5.9, 'High', 88, TRUE, '[-10.1, 14.9, 8.5, 6.9, 4.5, 3.3, 2.5, 1.9, 1.3, 1.0, 0.8, 0.5, 0.3]', NULL, NULL, NULL, NULL),
('evt-st02-trig-00', 'ST-02', DATE_SUB(NOW(), INTERVAL 15 MINUTE), '15min_triggered_event', 86, 3.88, 69.5, 82, 29.2, 84.5, -88, 6.2, 'High', 86, TRUE, '[-9.9, 14.7, 8.3, 6.8, 4.4, 3.2, 2.4, 1.8, 1.2, 0.9, 0.8, 0.4, 0.3]', NULL, NULL, NULL, NULL);
