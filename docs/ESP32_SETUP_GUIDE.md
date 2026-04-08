# Smart Irrigation System - ESP32 Setup & Configuration Guide

## 📋 Daftar Isi
1. [Requirements](#requirements)
2. [Arduino IDE Setup](#arduino-ide-setup)
3. [Library Installation](#library-installation)
4. [Hardware Wiring](#hardware-wiring)
5. [FirebaseConfiguration](#firebase-configuration)
6. [Code Configuration](#code-configuration)
7. [Testing & Debugging](#testing--debugging)
8. [Troubleshooting](#troubleshooting)

---

## Requirements

### Hardware
- **ESP32 Development Board** (DevKit v1 atau v4)
- **Relay Module** (5V, 1-channel minimum)
- **Soil Moisture Sensor** (Capacitive recommended for better accuracy)
- **LCD 16x2 dengan I2C Module** (Address: 0x27 atau 0x3F)
- **5V Power Supply** (minimal 2A untuk relay)
- **Connecting Cables** (Jumper wires)
- **Solenoid / Water Pump** (sesuai kebutuhan irigasi)

### Software
- Arduino IDE (versi 2.0 atau lebih baru)
- USB Cable untuk upload (Type-C atau Micro-USB sesuai ESP32)
- Firebase Project (Realtime Database)

---

## Arduino IDE Setup

### 1. Add ESP32 Board Manager
1. Buka **Arduino IDE** → **File → Preferences**
2. Di field "Additional Boards Manager URLs", tambahkan:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
3. Klik **OK**

### 2. Install ESP32 Board Pack
1. Buka **Tools → Board → Boards Manager**
2. Cari "esp32" dan pilih "ESP32 by Espressif Systems"
3. Klik **Install** (tunggu beberapa menit)

### 3. Select Board
1. **Tools → Board → ESP32 Arduino → ESP32 Dev Module**
2. **Tools → Port** → Pilih port COM tempat ESP32 terhubung

---

## Library Installation

### Gunakan Library Manager (Recommended)
1. Buka **Sketch → Include Library → Manage Libraries**
2. Cari dan install library berikut:

#### Library yang Dibutuhkan:
| Library | Author | Versi Min. |
|---------|--------|-----------|
| Firebase ESP32 Client | Mobitz | v4.4.0+ |
| LiquidCrystal_I2C | Frank de Brabander | v1.1.2+ |
| Wire | (Arduino Built-in) | - |
| WiFi | (Arduino Built-in) | - |

### Lembar Install Manual
Jika Library Manager tidak bekerja, download dari GitHub:

1. **Firebase ESP32 Client**
   - Download: https://github.com/mobizt/Firebase-ESP32
   - Extract ke `~/Arduino/libraries/`

2. **LiquidCrystal_I2C**
   - Download: https://github.com/johnrickman/LiquidCrystal_I2C
   - Extract ke `~/Arduino/libraries/`

---

## Hardware Wiring

### Pin Configuration

```
┌─────────────────────────────────────┐
│           ESP32 PIN OUT             │
├─────────────────────────────────────┤
│ GPIO 26  ──→ Relay Signal (IN)      │
│ GPIO 34  ──→ Soil Sensor (ADC)      │
│ GPIO 21  ──→ LCD I2C SDA            │
│ GPIO 22  ──→ LCD I2C SCL            │
│ GND      ──↔  Common GND            │
│ 5V/3.3V  ──→ (Sesuai modul)         │
└─────────────────────────────────────┘
```

### Wiring Diagram

#### 1. **Relay Module** (5V)
```
ESP32 GPIO 26 ───────→ Relay IN pin
ESP32 GND ───────────→ Relay GND
5V Supply ──────────→ Relay VCC

Relay COM ──────────→ Pump + (Live)
Relay NC/NO ────────→ Pump - (Neutral/Ground)
```

#### 2. **Soil Moisture Sensor** (Capacitive)
```
Sensor Pin "A0" ────→ ESP32 GPIO 34 (ADC)
Sensor VCC ─────────→ 3.3V atau 5V (cek sensor)
Sensor GND ─────────→ ESP32 GND
```

#### 3. **LCD 16x2 dengan I2C Module**
```
I2C SDA ─────────────→ ESP32 GPIO 21
I2C SCL ─────────────→ ESP32 GPIO 22
I2C VCC ─────────────→ 5V
I2C GND ─────────────→ ESP32 GND
```

### Contoh Koneksi Lengkap
```
┌──────────────┐
│   ESP32      │
├──────────────┤
│ GND (COM)────┼─────────┐
│ GPIO 26  ────┼──→ Relay Input
│ GPIO 34  ────┼──→ Soil Sensor
│ GPIO 21  ────┼──→ LCD SDA (I2C)
│ GPIO 22  ────┼──→ LCD SCL (I2C)
│ 3.3V ───┐    │
│ 5V  ────┼────┼──→ Relay VCC, Sensor VCC, LCD VCC
└────────────────┘
```

---

## Firebase Configuration

### 1. Create Firebase Project
1. Buka https://console.firebase.google.com/
2. Klik **Create Project** atau **Add Project**
3. Nama project: `smart-irrigation` (atau sesuai pilihan)
4. Pilih region (Asia Tenggara)
5. Klik **Create Project**

### 2. Create Realtime Database
1. Di Firebase Console, pilih project Anda
2. Klik **Realtime Database** (atau Build → Database untuk versi baru)
3. Klik **Create Database**
4. Pilih region terdekat
5. Mulai dalam mode **Test** (untuk developmen)
6. Klik **Enable**

### 3. Get Firebase Credentials
1. Di Project Settings (⚙️), klik **Service Accounts**
2. Klik **Generate New Private Key**
3. Buka file JSON yang didownload
4. Cari field `"database_url"` dan `"private_key_id"`
   - Copy `database_url` (ini adalah **FIREBASE_HOST**)
   - Contoh: `https://itprojek2-xxxxx-default-rtdb.firebaseio.com`

5. Di tab **Database Secrets**, copy secret key (ini adalah **FIREBASE_AUTH**)

### 4. Set Firebase Rules (Create Test Database Structure)

Di Firebase Console, buka **Database → Rules** dan set ke ini untuk development:

```json
{
  "rules": {
    "devices": {
      ".read": true,
      ".write": true,
      "{deviceId}": {
        "control": {
          ".read": true,
          ".write": true
        },
        "status": {
          ".read": true,
          ".write": true
        }
      }
    }
  }
}
```

> ⚠️ **PENTING**: Ini adalah setting TEST only. Untuk production, gunakan authentication yang proper!

---

## Code Configuration

### File: `smart_irrigation_esp32.ino`

#### Step 1: Sesuaikan WiFi Credentials
Cari bagian ini dan isi dengan data WiFi Anda:

```cpp
#define WIFI_SSID         "YOUR_WIFI_SSID"       // ← Ganti dengan SSID WiFi Anda
#define WIFI_PASSWORD     "YOUR_WIFI_PASSWORD"   // ← Ganti password WiFi
```

#### Step 2: Sesuaikan Firebase Configuration
```cpp
#define FIREBASE_HOST     "your-project-default-rtdb.firebaseio.com"  // ← Dari Firebase Console
#define FIREBASE_AUTH     "YOUR_DATABASE_SECRET_KEY"                  // ← Dari Database Secrets
```

#### Step 3: (Optional) Sesuaikan Pin Jika Berbeda

Jika Anda menggunakan pin berbeda dari default:

```cpp
#define RELAY_PIN         26    // Ganti jika berbeda
#define SOIL_PIN          34    // Ganti jika berbeda
#define LCD_SDA           21    // Ganti jika berbeda
#define LCD_SCL           22    // Ganti jika berbeda
```

#### Step 4: (Optional) Sesuaikan Sensor Threshold

```cpp
#define SOIL_THRESHOLD    30    // Mulai siram jika < 30%
#define SOIL_FULL         70    // Berhenti siram jika > 70%
#define MAX_PUMP_TIME     180   // Max durasi pompa: 3 menit
```

#### Step 5: (Optional) Ubah I2C Address LCD Jika Perlu

Jika LCD tidak menampilkan, coba address `0x3F`:

```cpp
LiquidCrystal_I2C lcd(0x27, 16, 2);  // Ganti 0x27 → 0x3F jika tidak bekerja
```

### Identifikasi I2C Address LCD

Gunakan **I2C Scanner** untuk menemukan address:

1. Copy kode di bawah ke Arduino IDE baru
2. Upload dan buka Serial Monitor
3. Lihat address yang ditemukan

```cpp
#include <Wire.h>

void setup() {
  Serial.begin(115200);
  Wire.begin(21, 22);  // SDA=21, SCL=22 (sesuaikan dengan pin Anda)
}

void loop() {
  for (byte i = 8; i < 120; i++) {
    Wire.beginTransmission(i);
    if (Wire.endTransmission() == 0) {
      Serial.print("Found I2C device at address: 0x");
      Serial.println(i, HEX);
    }
  }
  delay(3000);
}
```

---

## Upload Code ke ESP32

### 1. Hubungkan ESP32 dengan USB
2. Di Arduino IDE: **Sketch → Upload** atau tekan **Ctrl+U**
3. Tunggu sampai selesai (±30 detik)
4. Saat muncul "Connecting....______", tekan tombol **BOOT** di ESP32

Jika berhasil, akan muncul: `Hard resetting via RTS pin...`

---

## Testing & Debugging

### 1. Open Serial Monitor
1. **Tools → Serial Monitor** atau **Ctrl+Shift+M**
2. Set baud rate ke **115200**

### 2. Check Boot Message
```
========================================
SMART IRRIGATION SYSTEM - ESP32
Device ID: esp32_01
========================================

[SETUP] Relay pin initialized (LOW)
[LCD] Initialized
[WiFi] Connecting to: YOUR_WIFI_SSID
.....
[WiFi] Connected!
[WiFi] IP: 192.168.1.100
[Firebase] Initializing..
[Firebase] Connected & device set online!
[SETUP] Initialization complete!
```

### 3. Serial Commands untuk Testing

Ketik di Serial Monitor:

| Command | Fungsi |
|---------|--------|
| `P` | Nyalakan pompa |
| `S` | Matikan pompa |
| `M` | Tampilkan level kelembaban saat ini |
| `T` | Toggle mode Auto/Manual |
| `D` | Tampilkan status system lengkap |
| `R` | Soft restart ESP32 |
| `H` | Bantuan perintah |

### 4. Contoh Output yang Benar
```
[SENSOR] Raw: 2500 | Moisture: 45%
[Firebase] Moisture:45% | Pump:OFF
[PUMP] STARTED
[PUMP] STOPPED - Duration: 30 sec
```

---

## Troubleshooting

### ❌ ESP32 tidak connect ke WiFi
**Solusi:**
- Cek SSID dan password (case-sensitive)
- Pastikan WiFi 2.4 GHz (ESP32 tidak support 5GHz)
- Restart router
- Cek signal kekuatan terima (-30 dBm lebih bagus dari -90)

```cpp
// Cek kekuatan signal
Serial.println(WiFi.RSSI());  // Range: -100 (lemah) hingga -30 (kuat)
```

### ❌ Firebase tidak connect
**Solusi:**
- Cek `FIREBASE_HOST` dan `FIREBASE_AUTH` sesuai dengan database Anda
- Pastikan Firebase Rules sudah allow test mode
- Lihat Serial Monitor, cari error message: `Firebase Connection failed!`

```cppSerial out:
// Debug Firebase connection
Firebase.reconnectWiFi(true);
Firebase.begin(&config, &auth);
```

### ❌ LCD tidak menampilkan apa-apa (blank)
**Solusi:**
1. Cek wiring GPIO 21, 22
2. Identifikasi I2C address (gunakan I2C Scanner di atas)
3. Edit `0x27` → address yang benar
4. Cek koneksi GND dan 5V

### ❌ Pump tidak nyala
**Solusi:**
- Cek relay GPIO 26 sudah terhubung
- Test dengan serial command: `P` (seharusnya relay berbunyi "klik")
- Cek supply 5V ke relay
- Gunakan multimeter untuk test relay

### ❌ Sensor kelembaban selalu 0% atau 100%
**Solusi:**
- Cek pin GPIO 34 sudah terhubung ke sensor
- Calibrate sensor dengan merendamnya:
  - Dry: catat nilai
  - Wet (dalam air): catat nilai
  - Update `map()` di `readSoilMoisture()`

```cpp
// Saat ini: map(sensorRaw, 4095, 0, 0, 100)
// Jika berbeda calibration, sesuaikan:
// map(sensorRaw, DRY_VALUE, WET_VALUE, 0, 100)
```

### ❌ Serial data tidak muncul
**Solusi:**
- Cek baud rate 115200
- Tekan tombol RST di ESP32
- Cek USB cable (beberapa hanya untuk charging, bukan data)

---

## Firebase Database Structure

Struktur data yang akan dibuat otomatis:

```
devices/
├── esp32_01/
│   ├── control/
│   │   ├── manualPump: false            (bool)
│   │   ├── autoWatering: true           (bool)
│   │   └── lcdMessage: "text"           (string)
│   └── status/
│       ├── pumpRunning: false           (bool)
│       ├── moisture: 45                 (int)
│       ├── lastWatered: "14:30:45"      (string)
│       ├── lastDuration: 30             (int, detik)
│       ├── online: true                 (bool)
│       ├── deviceName: "Smart Pump 01"  (string)
│       └── lastUpdate: 1702000000       (timestamp)
```

---

## Tips & Best Practices

### 1. Calibration Sensor
Lakukan calibration pertama kali:
```cpp
// Dry soil dalam udara terbuka: nilai max
// Immerse sensor dalam air: nilai min
// Gunakan nilai tersebut untuk map() yang akurat
```

### 2. Max Pump Duration
Atur aman untuk pompa Anda (default 3 menit):
```cpp
#define MAX_PUMP_TIME 180  // seconds
```

### 3. WiFi Fallback
Jika WiFi terputus, pompa otomatis berhenti untuk safety.

### 4. Monitor via Serial
Selalu buka Serial Monitor untuk debugging saat development.

---

## Koneksi ke Android App

Android app mengirim perintah via Firebase:
- **Tombol "Siram Manual"** → set `control/manualPump = true`
- **Tombol "Hentikan"** → set `control/manualPump = false`
- **Toggle "Auto Mode"** → set `control/autoWatering = true/false`

ESP32 membaca perintah ini setiap 5 detik dan merespons accordingly.

---

## Contact & Support

Jika ada masalah atau pertanyaan, cek:
1. Serial Monitor output
2. Firebase Console → Database → Logs
3. Upload ulang code setelah perubahan
4. Restart ESP32 jika perlu (tombol RST)

**Good luck! 🚀**
