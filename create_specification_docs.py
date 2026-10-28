import os
import sys
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn
import win32com.client

def set_cell_background(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=120, bottom=120, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def set_table_borders(table, color="D1D5DB", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:insideV w:val="none"/>'
        f'<w:left w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

def add_code_block(doc, text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Inches(6.5)
    
    cell = table.cell(0, 0)
    set_cell_background(cell, "F8FAFC")
    set_cell_margins(cell, top=140, bottom=140, left=180, right=180)
    
    # Border for code block (light slate)
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>'
        f'<w:left w:val="single" w:sz="18" w:space="0" w:color="3B82F6"/>'
        f'<w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>'
        f'<w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(tcBorders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.15
    
    run = p.add_run(text)
    run.font.name = "Consolas"
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A) # Dark slate
    
    # Empty space after table
    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_before = Pt(0)
    p_after.paragraph_format.space_after = Pt(4)

def add_callout(doc, text, title="NOTE / BEST PRACTICE", border_color="0D9488", bg_color="F0FDFA"):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Inches(6.5)
    
    cell = table.cell(0, 0)
    set_cell_background(cell, bg_color)
    set_cell_margins(cell, top=120, bottom=120, left=160, right=160)
    
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/>'
        f'<w:left w:val="single" w:sz="24" w:space="0" w:color="{border_color}"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(tcBorders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    
    run_t = p.add_run(f"[{title}] ")
    run_t.bold = True
    run_t.font.name = "Arial"
    run_t.font.size = Pt(9.5)
    run_t.font.color.rgb = RGBColor(0x0F, 0x76, 0x6E)
    
    run_b = p.add_run(text)
    run_b.font.name = "Arial"
    run_b.font.size = Pt(9.5)
    run_b.font.color.rgb = RGBColor(0x1F, 0x29, 0x37)
    
    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_before = Pt(0)
    p_after.paragraph_format.space_after = Pt(4)

def build_document(docx_path):
    doc = Document()
    
    # Set standard margins (1 inch / 72pt)
    sections = doc.sections
    for s in sections:
        s.top_margin = Inches(0.8)
        s.bottom_margin = Inches(0.8)
        s.left_margin = Inches(0.85)
        s.right_margin = Inches(0.85)
    
    # ------------------ COVER / HEADER ------------------
    p_tag = doc.add_paragraph()
    p_tag.paragraph_format.space_before = Pt(0)
    p_tag.paragraph_format.space_after = Pt(4)
    run_tag = p_tag.add_run("TERMAGUARD.AI  |  FIRMWARE & HARDWARE INTEGRATION")
    run_tag.font.name = "Arial"
    run_tag.font.size = Pt(9)
    run_tag.font.bold = True
    run_tag.font.color.rgb = RGBColor(0x25, 0x63, 0xEB) # Blue 600
    
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(2)
    p_title.paragraph_format.space_after = Pt(4)
    run_title = p_title.add_run("ESP32 4G Telemetry Packet Specification")
    run_title.font.name = "Arial"
    run_title.font.size = Pt(22)
    run_title.font.bold = True
    run_title.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A) # Slate 900
    
    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(14)
    run_sub = p_sub.add_run("Edge Node HTTP Communication Protocol, Sensor Data Dictionary & Firmware Implementation Guidelines (Release V1.0)")
    run_sub.font.name = "Arial"
    run_sub.font.size = Pt(11)
    run_sub.font.color.rgb = RGBColor(0x47, 0x55, 0x69) # Slate 600
    
    # Meta Table
    meta_table = doc.add_table(rows=2, cols=4)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta_table.autofit = False
    col_widths = [Inches(1.6), Inches(1.8), Inches(1.5), Inches(1.6)]
    for row in meta_table.rows:
        for idx, width in enumerate(col_widths):
            row.cells[idx].width = width
            
    meta_data = [
        [("Doc Reference:", True), ("TG-SPEC-FW-4G-V1.0", False), ("Date:", True), ("October 2026", False)],
        [("Target Hardware:", True), ("ESP32 + 4G LTE Modem", False), ("Target Backend:", True), ("Spring Boot REST API", False)]
    ]
    for r_idx, row in enumerate(meta_data):
        for c_idx, (text, is_bold) in enumerate(row):
            cell = meta_table.cell(r_idx, c_idx)
            set_cell_background(cell, "F1F5F9" if is_bold else "FFFFFF")
            set_cell_margins(cell, top=60, bottom=60, left=100, right=100)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            run = p.add_run(text)
            run.font.name = "Arial"
            run.font.size = Pt(8.5)
            run.bold = is_bold
            run.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)
            
    set_table_borders(meta_table, color="CBD5E1", sz="4")
    
    doc.add_paragraph().paragraph_format.space_after = Pt(10)
    
    # ------------------ SECTION 1 ------------------
    h1 = doc.add_paragraph()
    h1.paragraph_format.space_before = Pt(14)
    h1.paragraph_format.space_after = Pt(4)
    run_h1 = h1.add_run("1. System Overview & Network Transport Protocol")
    run_h1.font.name = "Arial"
    run_h1.font.size = Pt(14)
    run_h1.font.bold = True
    run_h1.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A) # Deep Navy
    
    p_desc = doc.add_paragraph()
    p_desc.paragraph_format.space_after = Pt(6)
    p_desc.paragraph_format.line_spacing = 1.15
    run = p_desc.add_run(
        "The TermaGuard edge node is an ESP32-based autonomous sensor node deployed across commercial tree plantations. "
        "It communicates directly with the central cloud/edge gateway via a 4G LTE-Cat1 or Cat-M1 cellular modem (e.g. SIMCom SIM7600/A7670 or Quectel EC200U) "
        "over a standard HTTP POST REST interface."
    )
    run.font.name = "Arial"
    run.font.size = Pt(10)
    
    # Network Parameter Table
    net_table = doc.add_table(rows=6, cols=2)
    net_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    net_table.columns[0].width = Inches(2.0)
    net_table.columns[1].width = Inches(4.5)
    
    net_rows = [
        ("Transport Protocol", "HTTP/1.1 POST over TCP/IP 4G Cellular Link"),
        ("Target Ingestion URL", "http://<SERVER_IP>:8080/api/v1/telemetry/uplink"),
        ("Payload Content-Type", "application/json"),
        ("Authentication Header", "X-Node-API-Key: termiguard-node-secret-2026"),
        ("ESP32 User-Agent", "TermaGuard-Node/1.0 (ESP32-4G)"),
        ("Server Response Code", "200 OK with parameter synchronization JSON body")
    ]
    for r_idx, (k, v) in enumerate(net_rows):
        c0 = net_table.cell(r_idx, 0)
        c1 = net_table.cell(r_idx, 1)
        set_cell_background(c0, "F8FAFC")
        set_cell_background(c1, "FFFFFF")
        set_cell_margins(c0, top=60, bottom=60, left=100, right=100)
        set_cell_margins(c1, top=60, bottom=60, left=100, right=100)
        
        p0 = c0.paragraphs[0]
        p0.paragraph_format.space_before = Pt(0)
        p0.paragraph_format.space_after = Pt(0)
        r0 = p0.add_run(k)
        r0.font.name = "Arial"
        r0.font.size = Pt(9)
        r0.bold = True
        
        p1 = c1.paragraphs[0]
        p1.paragraph_format.space_before = Pt(0)
        p1.paragraph_format.space_after = Pt(0)
        r1 = p1.add_run(v)
        r1.font.name = "Consolas" if ("http" in v or "X-" in v or "application" in v) else "Arial"
        r1.font.size = Pt(9)
        if "http" in v:
            r1.font.color.rgb = RGBColor(0x1D, 0x4E, 0xD8)
            
    set_table_borders(net_table, color="E2E8F0")
    doc.add_paragraph().paragraph_format.space_after = Pt(8)
    
    # ------------------ SECTION 2 ------------------
    h2 = doc.add_paragraph()
    h2.paragraph_format.space_before = Pt(14)
    h2.paragraph_format.space_after = Pt(4)
    run_h2 = h2.add_run("2. Operating Modes & Edge State Machine")
    run_h2.font.name = "Arial"
    run_h2.font.size = Pt(14)
    run_h2.font.bold = True
    run_h2.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)
    
    p_modes = doc.add_paragraph()
    p_modes.paragraph_format.space_after = Pt(6)
    p_modes.paragraph_format.line_spacing = 1.15
    run = p_modes.add_run(
        "To maximize battery endurance over multi-year field deployments, the firmware employs an environmental gating strategy: "
        "subterranean termites require high humidity and moist soil (>65%) to construct shelter mud tubes. When soil is dry, acoustic circuits are gated off."
    )
    run.font.name = "Arial"
    run.font.size = Pt(10)
    
    # Mode cards
    add_callout(
        doc,
        "Awakens every 15 minutes. Triggered when Soil Moisture >= 65.0%. "
        "The ESP32 powers up the analog piezo acoustic circuit via a low-dropout switch, records a 5-10s audio window, "
        "calculates the acoustic energy score (0-100) and 13-band Mel-Frequency Cepstral Coefficients (MFCC), "
        "transmits the full payload over 4G, and returns to deep sleep for 15 minutes.",
        title="MODE 1: ACTIVE 15-MINUTE TRIGGERED SAMPLING",
        border_color="2563EB",
        bg_color="EFF6FF"
    )
    
    add_callout(
        doc,
        "Awakens once every 24 hours (or configurable heartbeat interval). Triggered when Soil Moisture < 65.0%. "
        "Termites do not forage in dry soil; hence the acoustic sensing circuit is held in hard power-down to preserve battery. "
        "The node sends battery voltage, battery percentage, microclimate, and cellular link metrics (RSSI/SNR). "
        "Fields 'acousticScore' and 'mfccFeatures' MUST be set to null. Deep sleep resumes for 24 hours.",
        title="MODE 2: 24-HOUR DORMANT HEARTBEAT (KEEPALIVE)",
        border_color="0D9488",
        bg_color="F0FDFA"
    )
    
    # ------------------ SECTION 3 ------------------
    h3 = doc.add_paragraph()
    h3.paragraph_format.space_before = Pt(14)
    h3.paragraph_format.space_after = Pt(4)
    run_h3 = h3.add_run("3. Telemetry Payload Data Dictionary")
    run_h3.font.name = "Arial"
    run_h3.font.size = Pt(14)
    run_h3.font.bold = True
    run_h3.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)
    
    p_dict = doc.add_paragraph()
    p_dict.paragraph_format.space_after = Pt(6)
    run = p_dict.add_run("All fields conform to the following schema. The backend parser natively supports both camelCase and snake_case keys.")
    run.font.name = "Arial"
    run.font.size = Pt(10)
    
    # Dictionary Table
    dict_table = doc.add_table(rows=12, cols=5)
    dict_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    col_w = [Inches(1.5), Inches(0.9), Inches(0.9), Inches(1.2), Inches(2.0)]
    for row in dict_table.rows:
        for idx, width in enumerate(col_w):
            row.cells[idx].width = width
            
    headers = ["Field Key", "Type", "Required", "Units / Range", "Description & Implementation Notes"]
    for idx, h_text in enumerate(headers):
        cell = dict_table.cell(0, idx)
        set_cell_background(cell, "1E3A8A")
        set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(h_text)
        run.font.name = "Arial"
        run.font.size = Pt(8.5)
        run.bold = True
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        
    dict_data = [
        ("stationId", "String", "YES", "e.g. \"ST-01\"", "Unique hardware node ID matching station label / barcode."),
        ("eventType", "String", "YES", "\"15min_triggered_event\" | \"daily_heartbeat\"", "Determines whether packet is an active detection or sleep keepalive."),
        ("batteryVoltage", "Float", "YES", "3.30 to 4.25 (V)", "Measured ADC battery voltage under moderate load."),
        ("batteryLevel", "Integer", "YES", "0 to 100 (%)", "Battery charge percentage estimated via lookup table / fuel gauge."),
        ("soilMoisture", "Float", "YES", "0.0 to 100.0 (%)", "Volumetric water content from capacitive moisture probe."),
        ("acousticScore", "Integer", "OPTIONAL", "0 to 100 (or null)", "Bioacoustic energy amplitude score. NULL in heartbeat mode."),
        ("temperature", "Float", "YES", "°Celsius (e.g. 28.5)", "Microclimate ambient temperature measured near base of tree trunk."),
        ("humidity", "Float", "YES", "0.0 to 100.0 (%)", "Relative air humidity measured by digital sensor (SHT31 / AHT20)."),
        ("isMoistureTriggered", "Boolean", "YES", "true | false", "Flag indicating whether soil moisture exceeded activation threshold."),
        ("mfccFeatures", "Array", "OPTIONAL", "13 Floats (or null)", "13-band Mel-Frequency Cepstral Coefficients. NULL in heartbeat."),
        ("rssi", "Integer", "YES", "-115 to -40 (dBm)", "Cellular link signal strength reported by modem AT+CSQ.")
    ]
    
    for r_idx, row_vals in enumerate(dict_data):
        row = dict_table.rows[r_idx + 1]
        bg = "F8FAFC" if r_idx % 2 == 1 else "FFFFFF"
        for c_idx, val in enumerate(row_vals):
            cell = row.cells[c_idx]
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            run = p.add_run(val)
            run.font.name = "Consolas" if c_idx == 0 else "Arial"
            run.font.size = Pt(8.5)
            if c_idx == 2:
                run.bold = True
                run.font.color.rgb = RGBColor(0xDC, 0x26, 0x26) if val == "YES" else RGBColor(0x64, 0x74, 0x8B)
                
    set_table_borders(dict_table, color="E2E8F0")
    doc.add_paragraph().paragraph_format.space_after = Pt(8)
    
    # ------------------ SECTION 4 ------------------
    h4 = doc.add_paragraph()
    h4.paragraph_format.space_before = Pt(14)
    h4.paragraph_format.space_after = Pt(4)
    run_h4 = h4.add_run("4. Concrete JSON Payload Examples")
    run_h4.font.name = "Arial"
    run_h4.font.size = Pt(14)
    run_h4.font.bold = True
    run_h4.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)
    
    p_ex1 = doc.add_paragraph()
    p_ex1.paragraph_format.space_after = Pt(2)
    r = p_ex1.add_run("4.1 Example: Active 15-Minute Triggered Packet (Acoustic Detection Active)")
    r.bold = True
    r.font.name = "Arial"
    r.font.size = Pt(10)
    
    code_ex1 = (
        '{\n'
        '  "stationId": "ST-01",\n'
        '  "eventType": "15min_triggered_event",\n'
        '  "batteryVoltage": 3.92,\n'
        '  "batteryLevel": 88,\n'
        '  "soilMoisture": 72.5,\n'
        '  "acousticScore": 84,\n'
        '  "temperature": 29.1,\n'
        '  "humidity": 83.4,\n'
        '  "rssi": -76,\n'
        '  "snr": 7.8,\n'
        '  "isMoistureTriggered": true,\n'
        '  "mfccFeatures": [\n'
        '    -10.2, 14.5, 8.1, 6.3, 4.2, 3.1, 2.5, 1.9, 1.2, 0.8, 0.6, 0.4, 0.2\n'
        '  ]\n'
        '}'
    )
    add_code_block(doc, code_ex1)
    
    p_ex2 = doc.add_paragraph()
    p_ex2.paragraph_format.space_after = Pt(2)
    r = p_ex2.add_run("4.2 Example: 24-Hour Dormant Heartbeat Keepalive (Soil Dry, Sensor Gated Off)")
    r.bold = True
    r.font.name = "Arial"
    r.font.size = Pt(10)
    
    code_ex2 = (
        '{\n'
        '  "stationId": "ST-01",\n'
        '  "eventType": "daily_heartbeat",\n'
        '  "batteryVoltage": 4.10,\n'
        '  "batteryLevel": 97,\n'
        '  "soilMoisture": 42.0,\n'
        '  "acousticScore": null,\n'
        '  "temperature": 27.8,\n'
        '  "humidity": 65.2,\n'
        '  "rssi": -80,\n'
        '  "snr": 8.5,\n'
        '  "isMoistureTriggered": false,\n'
        '  "mfccFeatures": null\n'
        '}'
    )
    add_code_block(doc, code_ex2)
    
    # ------------------ SECTION 5 ------------------
    h5 = doc.add_paragraph()
    h5.paragraph_format.space_before = Pt(14)
    h5.paragraph_format.space_after = Pt(4)
    run_h5 = h5.add_run("5. Server Response & Downlink Configuration Synchronization")
    run_h5.font.name = "Arial"
    run_h5.font.size = Pt(14)
    run_h5.font.bold = True
    run_h5.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)
    
    p_resp = doc.add_paragraph()
    p_resp.paragraph_format.space_after = Pt(6)
    p_resp.paragraph_format.line_spacing = 1.15
    run = p_resp.add_run(
        "Upon successful validation and database insertion, the server returns an HTTP 200 OK containing an acknowledgment "
        "and a nested 'config' object. The ESP32 firmware SHOULD inspect this object to dynamically adjust its local thresholds."
    )
    run.font.name = "Arial"
    run.font.size = Pt(10)
    
    code_resp = (
        'HTTP/1.1 200 OK\n'
        'Content-Type: application/json\n\n'
        '{\n'
        '  "status": "SUCCESS",\n'
        '  "message": "Telemetry packet accepted and processed",\n'
        '  "eventId": "evt-1793173512813-686",\n'
        '  "stationId": "ST-01",\n'
        '  "serverTime": "2026-10-28 15:45:12",\n'
        '  "config": {\n'
        '    "moistureThreshold": 65.0,\n'
        '    "samplingFrequencyMinutes": 15,\n'
        '    "heartbeatIntervalHours": 24\n'
        '  }\n'
        '}'
    )
    add_code_block(doc, code_resp)
    
    add_callout(
        doc,
        "By parsing the 'config' block from the HTTP response, field nodes achieve dynamic OTA parameter synchronization "
        "(e.g., agronomy team alters threshold from 65% to 70% in the web UI) without establishing a power-hungry persistent "
        "MQTT downlink channel or waiting for SMS commands.",
        title="FIRMWARE ADVANTAGE: PIGGYBACK DOWNLINK SYNC",
        border_color="059669",
        bg_color="ECFDF5"
    )
    
    # ------------------ SECTION 6 ------------------
    h6 = doc.add_paragraph()
    h6.paragraph_format.space_before = Pt(14)
    h6.paragraph_format.space_after = Pt(4)
    run_h6 = h6.add_run("6. Firmware Implementation & Cellular Modem AT Sequence")
    run_h6.font.name = "Arial"
    run_h6.font.size = Pt(14)
    run_h6.font.bold = True
    run_h6.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)
    
    p_at = doc.add_paragraph()
    p_at.paragraph_format.space_after = Pt(6)
    p_at.paragraph_format.line_spacing = 1.15
    run = p_at.add_run(
        "For teams using raw AT commands via hardware UART on modems such as SIMCom SIM7600/A7670 or Quectel EC200U, "
        "the standard HTTP POST execution sequence is provided below:"
    )
    run.font.name = "Arial"
    run.font.size = Pt(10)
    
    code_at = (
        '// 1. Check Network Registration\n'
        'AT+CGREG?                  --> Expect: +CGREG: 0,1 or 0,5 (Registered)\n'
        'AT+CSQ                     --> Expect: +CSQ: <rssi>,<ber> (e.g. 24,99)\n\n'
        '// 2. Set APN (Carrier Specific)\n'
        'AT+CGDCONT=1,"IP","<YOUR_CARRIER_APN>"\n\n'
        '// 3. Initialize HTTP Session\n'
        'AT+HTTPINIT\n'
        'AT+HTTPPARA="CID",1\n'
        'AT+HTTPPARA="URL","http://<SERVER_IP>:8080/api/v1/telemetry/uplink"\n'
        'AT+HTTPPARA="CONTENT","application/json"\n'
        'AT+HTTPPARA="USERDATA","X-Node-API-Key: termiguard-node-secret-2026"\n\n'
        '// 4. Send JSON Payload (Length in Bytes, Timeout in ms)\n'
        'AT+HTTPDATA=<PAYLOAD_LENGTH>,10000\n'
        '--> Wait for "DOWNLOAD" prompt, then write JSON payload bytes\n'
        '--> Expect: "OK"\n\n'
        '// 5. Trigger POST Action\n'
        'AT+HTTPACTION=1            --> Expect: +HTTPACTION: 1,200,<RESPONSE_LENGTH>\n\n'
        '// 6. Read Server Response & Config Sync\n'
        'AT+HTTPREAD                --> Output response JSON\n'
        'AT+HTTPTERM                --> Close HTTP session\n\n'
        '// 7. Power down 4G Modem & Sleep ESP32\n'
        'AT+CPOWD=1                 --> Graceful modem shutdown\n'
        'esp_deep_sleep_start();    --> ESP32 enters deep sleep'
    )
    add_code_block(doc, code_at)
    
    # Retry policy callout
    add_callout(
        doc,
        "If the cellular modem fails to register or the HTTP POST times out (e.g. tower congestion): "
        "(1) Do NOT retry continuously in a tight loop as this will rapidly deplete the LiSOCl2 battery; "
        "(2) Wait 60 seconds and attempt a single retry (maximum 3 attempts); "
        "(3) If connection still fails, save packet to SPIFFS flash memory and enter deep sleep until next cycle.",
        title="CRITICAL POWER PROTECTION: RETRY POLICY",
        border_color="E11D48",
        bg_color="FFF1F2"
    )
    
    # Save document
    doc.save(docx_path)
    print(f"Successfully generated Word document: {docx_path}")

def convert_to_pdf(docx_path, pdf_path):
    print(f"Converting Word document to PDF: {pdf_path}")
    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    try:
        doc = word.Documents.Open(os.path.abspath(docx_path))
        wdFormatPDF = 17
        doc.SaveAs(os.path.abspath(pdf_path), FileFormat=wdFormatPDF)
        doc.Close()
        print(f"Successfully generated PDF document: {pdf_path}")
    finally:
        word.Quit()

if __name__ == "__main__":
    output_dir = r"d:\重要文件\IDP project"
    docx_file = os.path.join(output_dir, "TermaGuard_ESP32_4G_Telemetry_Packet_Specification_v1.0.docx")
    pdf_file = os.path.join(output_dir, "TermaGuard_ESP32_4G_Telemetry_Packet_Specification_v1.0.pdf")
    
    build_document(docx_file)
    convert_to_pdf(docx_file, pdf_file)
