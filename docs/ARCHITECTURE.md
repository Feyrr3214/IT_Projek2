# Smart Irrigation System - Architecture & Communication Protocol

## 🏗️ System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    SMART IRRIGATION SYSTEM                      │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐                ┌──────────────────────────┐
│  Android App     │◄───WiFi────►   │  ESP32 Controller        │
│  (User Control)  │                │  (Hardware Management)    │
└──────────────────┘                └──────────────────────────┘
        │                                      │
        │                                      │
        │           (Firebase Cloud)           │
        │         Realtime Database            │
        └──────────────────┬───────────────────┘
                           │
                    [  Database  ]
                    [   Server   ]
                           │
        ┌──────────────────┴───────────────────┐
        │                                      │
   ┌────▼────┐                          ┌────▼────┐
   │ Commands │                          │ Sensor  │
   │  (From   │                          │ Data    │
   │ Android) │                          │ (From   │
   │          │                          │ ESP32)  │
   └──────────┘                          └─────────┘

Hardware Components (ESP32-connected):
┌─────────────────────────────────────────────────────────────┐
│                       ESP32 DevKit                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Relay      │  │ Soil Moisture│  │ LCD Display  │    │
│  │   (Pump)     │  │   Sensor     │  │  (I2C)       │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│    GPIO 26           GPIO 34           GPIO 21/22         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow & Communication

### 1️⃣ Initialization Phase

```
[START] → Initialize Pins → Initialize LCD → Connect WiFi 
   ↓                                              ↓
                                        Setup Firebase Connection
   ↓                                              ↓
[SET STATUS] → devices/esp32_01/status/online = true
   ↓
LCD: "System Ready!"
```

**ESP32 Serial Output:**
```
[SETUP] Relay pin initialized (LOW)
[LCD] Initialized
[WiFi] Connected! IP: 192.168.1.100
[Firebase] Connected & device set online!
[SETUP] Initialization complete!
```

---

### 2️⃣ Continuous Operation Loop

Setiap iteration loop (~100ms):

```
┌─────────────────────────────────────────────────────────────┐
│                     MAIN LOOP CYCLE                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ├─ READ SENSOR                                            │
│  │  └─ analogRead(GPIO 34)                                │
│  │     └─ Convert to Moisture %                           │
│  │                                                         │
│  ├─ READ FIREBASE COMMANDS (setiap 5 detik)              │
│  │  ├─ manualPump ?                                       │
│  │  ├─ autoWatering ?                                     │
│  │  └─ lcdMessage ?                                       │
│  │                                                         │
│  ├─ EXECUTE CONTROL LOGIC                                 │
│  │  ├─ IF manual_pump = true                              │
│  │  │  └─ START PUMP                                      │
│  │  ├─ ELSE IF auto_watering & moisture < 30%             │
│  │  │  └─ START PUMP                                      │
│  │  └─ ELSE IF moisture > 70%                             │
│  │     └─ STOP PUMP                                       │
│  │                                                         │
│  ├─ UPDATE FIREBASE STATUS (setiap 2 detik)              │
│  │  ├─ moisture = current_value                           │
│  │  ├─ pumpRunning = true/false                           │
│  │  ├─ online = true                                      │
│  │  └─ lastUpdate = timestamp                             │
│  │                                                         │
│  └─ UPDATE LCD DISPLAY (setiap 3 detik)                  │
│     └─ Show: "PUMP: ON", "M: 45%", etc                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Timing:
├─ Sensor Read:     Every 100ms  (main loop)
├─ Firebase Read:   Every 5 sec  (commands)
├─ Firebase Write:  Every 2 sec  (status)
└─ LCD Update:      Every 3 sec  (display)
```

---

### 3️⃣ Manual Pump Activation (from Android)

**Sequence:**

```
[User taps "Siram Sekarang" button]
        ↓
[Android App]
        ↓
Write to Firebase:
  devices/esp32_01/control/manualPump = true
  devices/esp32_01/control/lcdMessage = "Siram Manual|Sedang Jalan"
        ↓
[~5 detik later, ESP32 checks]
        ↓
readFirebaseCommands() reads manualPump = true
        ↓
executeControlLogic():
  state.manualPump == true
  → shouldPumpRun = true
        ↓
[Relay aktif (GPIO 26 → HIGH)]
        ↓
Serial: "[PUMP] STARTED"
LCD: "PUMP: RUNNING"
        ↓
[Pompa air hidup]


[User taps "Hentikan" button]
        ↓
Write to Firebase:
  devices/esp32_01/control/manualPump = false
        ↓
[~5 detik later]
        ↓
readFirebaseCommands() reads manualPump = false
executeControlLogic(): shouldPumpRun = false
        ↓
stopPump():
  - Relay off (GPIO 26 → LOW)
  - Duration calculated
  - Saved to Firebase
  - LCD updated
        ↓
Serial: "[PUMP] STOPPED - Duration: 30 sec"
LCD: "PUMP: STOPPED"
```

**Firebase Path:**
```
devices/
└── esp32_01/
    ├── control/
    │   ├── manualPump: false → true (command dari Android)
    │   └── lcdMessage: "Siram Manual|Sedang Jalan"
    └── status/
        ├── pumpRunning: false → true (feedback dari ESP32)
        ├── lastWatered: "14:30:45"
        └── lastDuration: 30
```

---

### 4️⃣ Auto Watering Mode

**Jika `control/autoWatering = true`:**

```
Loop:
  ├─ Read moisture level (e.g., 25%)
  │
  ├─ IF moisture < 30% (SOIL_THRESHOLD)
  │  ├─ START PUMP
  │  └─ Continue checking...
  │
  └─ WHILE pumpRunning:
     ├─ Monitor moisture increasing
     ├─ IF moisture > 70% (SOIL_FULL)
     │  └─ STOP PUMP
     ├─ IF pumpRunning > 3 minutes (MAX_PUMP_TIME)
     │  └─ EMERGENCY STOP (safety)
     └─ Update Firebase every 2 sec
```

**Timeline Example:**

```
Time: 14:00  Moisture: 25% → START
             Pump running...
             
Time: 14:01  Moisture: 45% → keep running
             
Time: 14:02  Moisture: 65% → keep running
             
Time: 14:03  Moisture: 72% → STOP
             Duration: 180 seconds
             Saved to Firebase
```

---

## 📡 Firebase Database Real-time Sync

### Write Operations (ESP32 → Firebase)

```
Every ~2 seconds:

Firebase.setInt(firebaseData,
                "devices/esp32_01/status/moisture", 45);
Firebase.setBool(firebaseData,
                 "devices/esp32_01/status/pumpRunning", true);
Firebase.setBool(firebaseData,
                 "devices/esp32_01/status/online", true);

Result in Firebase:
{
  "devices": {
    "esp32_01": {
      "status": {
        "moisture": 45,          ← Updated every 2 sec
        "pumpRunning": true,     ← Updated every 2 sec
        "online": true,          ← Updated every 2 sec
        "lastUpdate": 1702000000 ← Timestamp
      }
    }
  }
}
```

### Read Operations (Firebase → ESP32)

```
Every ~5 seconds:

String controlPath = "devices/esp32_01/control";

Firebase.getBool(firebaseData,
                 controlPath + "/manualPump");
boolean = firebaseData.boolData();  // true/false

Firebase.getBool(firebaseData,
                 controlPath + "/autoWatering");
    
Firebase.getString(firebaseData,
                   controlPath + "/lcdMessage");
```

---

## 🎮 Control Modes

### Mode 1: Manual Control

```
Android User Interface:
┌──────────────┐
│ ▶ Siram      │ ← User tap
│   Sekarang   │
└──────────────┘
        ↓
Firebase: manualPump = true
        ↓
ESP32: START PUMP immediately
        ↓
User tap "Hentikan" or timeout
        ↓
Firebase: manualPump = false
        ↓
ESP32: STOP PUMP
```

### Mode 2: Auto Watering

```
Android User Interface:
┌──────────────┐
│ 🔄 Auto Mode │ ← Toggle ON
│ [ON]  [OFF]  │
└──────────────┘
        ↓
Firebase: autoWatering = true
        ↓
ESP32 Loop:
  MONITOR moisture continuously
  ├─ If < 30% → AUTO START PUMP
  └─ If > 70% → AUTO STOP PUMP
        ↓
Repeat until user toggle OFF
```

---

## 🔐 Data Integrity & Safety

### Safety Mechanisms

```
1. WiFi Disconnection:
   └─ If WiFi lost while pump running
      └─ EMERGENCY STOP (prevent infinite running)
      
2. Maximum Pump Duration:
   └─ If pumpRunning > 3 minutes
      └─ FORCE STOP (prevent overflow/damage)
      
3. Sensor Validation:
   └─ Moisture constrained 0-100%
      └─ Invalid readings filtered
      
4. Firebase Retry:
   └─ If Firebase connection fails
      └─ Auto-reconnect every 5 sec
      └─ Payload resent on success
```

### Error Handling

```
┌─ Firebase Connection Error
│  ├─ Increment failureCount
│  ├─ If failureCount ≥ 3
│  │  └─ Mark firebaseConnected = false
│  └─ Retry on next cycle
|
├─ Sensor Reading Error
│  └─ Constrain to valid range (0-100%)
│
├─ Pin Logic Error
│  ├─ Check digitalWrite() result
│  └─ Log to Serial
│
└─ LCD Display Error
   └─ Try again next cycle (non-blocking)
```

---

## 📊 Example Real-world Scenario

**Time: 08:00 AM - System Start**

```
08:00:00 [SETUP] System boots, reads configuration
08:00:10 [WiFi] Connected to home_network
08:00:15 [Firebase] Connected, online = true
08:00:20 LCD: "System Ready! / Waiting..."
         
         Soil Moisture Sensor initial: 50%
         Manual Control: OFF
         Auto Mode: ON
         
08:00:30 [Firebase] Moisture: 50% | Pump: OFF ← uploaded
         
         Soil starts drying (irrigation needed)
         
08:15:00 Moisture drops to 28%
08:15:05 [SENSOR] Moisture: 28% < 30% threshold
         [PUMP] STARTED (auto mode)
         LCD: "PUMP: RUNNING / M: 28%"
         Firebase: pumpRunning = true
         
         Water flows from pump
         
08:16:00 Moisture: 45% (still < 70%, pump continues)
         
08:17:00 Moisture: 62% (getting closer)
         
08:18:00 Moisture: 72% > 70% (SOIL_FULL reached)
         [PUMP] STOPPED - Duration: 180 sec
         LCD: "PUMP: STOPPED / Dur: 180s"
         Firebase: lastDuration = 180
                   lastWatered = "08:18:00"
                   pumpRunning = false
         
08:18:30 System back to monitoring
         Moisture: 72% (sufficient)
         Waiting for next dry cycle
         
14:00:00 [User on Android App]
         Manual Control: manualPump = true
         
14:00:05 [PUMP] STARTED (manual override)
         LCD: "PUMP: RUNNING / M: 72%"
         
14:00:30 [User releases control]
         manualPump = false
         
14:00:35 [PUMP] STOPPED (if duration allows)
         OR continues if auto mode re-engages
         
18:00:00 Moisture back to 35%
         Auto mode kicks in again...
```

**Firebase State at 08:18:30:**

```json
{
  "devices": {
    "esp32_01": {
      "control": {
        "autoWatering": true,
        "lcdMessage": "",
        "manualPump": false
      },
      "status": {
        "deviceName": "Smart Pump 01",
        "lastDuration": 180,
        "lastUpdate": 1629279510,
        "lastWatered": "08:18:00",
        "moisture": 72,
        "online": true,
        "pumpRunning": false
      }
    }
  }
}
```

---

## 🔌 Power & Performance

### Power Consumption Estimate
```
ESP32 idle:        ~10 mA
ESP32 + LCD:       ~30 mA
WiFi active:       +50 mA
Relay coil:        ~70 mA
Pump motor:        ~500-2000 mA (varies)

Total with pump:   ~600-2000 mA @ 5V
```

### Performance Metrics
```
Sensor update:     Every 100ms (fast, local)
Firebase sync:     Every 2-5 seconds (cloud delay ~100-500ms)
Total latency:     ~6 seconds (worst case)
  - User tap Android → Firebase update
  - ESP32 reads Firebase
  - Relay responds
  - Feedback to Firebase
```

---

## 🐛 Debugging Points

### Serial Monitor Key Messages
```
✅ Boot OK:
   [WiFi] Connected! IP: 192.168.x.x
   [Firebase] Connected & device set online!
   
✅ Operation OK:
   [SENSOR] Raw: 2500 | Moisture: 45%
   [Firebase] Moisture:45% | Pump:OFF
   
⚠️ Issues:
   [WiFi] FAILED to connect!
   [Firebase] Connection failed!
   [SAFETY] Max pump time exceeded
   [ERROR] WiFi disconnected - Emergency stop pump!
```

### Firebase Console Monitoring
```
Real-time view shows:
- devices/esp32_01/status/moisture ← Changes every 2 sec
- devices/esp32_01/status/pumpRunning ← Changes on pump start/stop
- devices/esp32_01/status/online ← Should be true when running

Write commands from Android visible in:
- devices/esp32_01/control/manualPump ← Changes on user tap
```

---

## 📱 Android Integration Points

The Android app should:
1. **Read** from `devices/esp32_01/status/`
   - Display moisture gauge
   - Show pump status
   - Display last watered time
   
2. **Write** to `devices/esp32_01/control/`
   - manualPump on "Siram" button tap
   - autoWatering on toggle
   - lcdMessage for custom LCD text

3. **Monitor** real-time updates
   - Use Firebase listeners
   - Update UI immediately on changes

---

**End of Architecture Document**

For questions, refer to code comments and Serial Monitor output for debugging!
