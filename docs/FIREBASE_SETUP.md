# Firebase Realtime Database - Initialization & Testing Guide

## 📋 Tabel Isi
1. [Firebase Project Creation](#firebase-project-creation)
2. [Realtime Database Setup](#realtime-database-setup)
3. [Get Credentials](#get-credentials)
4. [Database Security Rules](#database-security-rules)
5. [Initialize Database Structure](#initialize-database-structure)
6. [Testing Data Flow](#testing-data-flow)

---

## Firebase Project Creation

### Step 1: Access Firebase Console
1. Buka https://console.firebase.google.com/
2. Login dengan Google Account
3. Klik **"Create Project"** atau **"Add Project"**

### Step 2: Project Configuration
1. **Project Name:** `smart-irrigation` (atau pilihan Anda)
2. **Analytics:** Disable untuk development (bisa di-enable nanti)
3. Klik **"Create Project"**
   - Tunggu 1-2 menit sampe siap

### Step 3: Project Overview
Setelah created, Anda akan di halaman Project Overview. Di sini ada berbagai service:
- Authentication
- Realtime Database ← **Kita pakai ini**
- Firestore
- Storage
- dsb

---

## Realtime Database Setup

### Step 1: Create Realtime Database
1. Di sidebar kiri, cari **"Realtime Database"**
   - Jika tidak terlihat, klik **"Build"** → cari **"Realtime Database"**
2. Klik **"Create Database"**

### Step 2: Database Configuration
1. **Location:** Pilih yang **paling dekat** (untuk Asia Tenggara: Singapore `asia-southeast1`)
2. **Security Rules:** Pilih **"Start in Test Mode"**
   - Test mode memberikan akses full untuk 30 hari (cukup untuk development)
3. Klik **"Enable"**

### Step 3: Database Ready
Halaman akan menampilkan:
```
Database URL: https://PROJECT-ID-default-rtdb.firebaseio.com
↑ Copy ini, ini adalah FIREBASE_HOST
```

---

## Get Credentials

### Method 1: Get Database Secret (Recommended untuk ESP32)

1. Di menu Realtime Database, klik tombol **⚙️ (Settings)**
2. Pilih tab **"Service Accounts"** (bukan Admin SDK)
3. Klik **"Database Secrets"** → bagian atas page
4. Seharusnya ada satu secret dengan status aktif
5. **Copy** secret tersebut → ini adalah **FIREBASE_AUTH**

Format akan seperti ini (contoh):
```
ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqr
```

### Method 2: Get API Key (alternatif)
1. Di **Project Settings** (⚙️ icon di sidebar)
2. Tab "General"
3. Cari **"Web API Key"**
4. Copy value-nya

### Summary Credentials Needed:
```
FIREBASE_HOST = https://smart-irrigation-xxxxx-default-rtdb.firebaseio.com
                 (dari Realtime Database URL)

FIREBASE_AUTH = ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqr
                 (dari Database Secrets)
```

---

## Database Security Rules

### Set Rules (Important!)

1. Di **Realtime Database**, klik tab **"Rules"**
2. Hapus semua kode yang ada
3. Paste kode berikut (untuk development/test mode):

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

4. Klik **"Publish"**

### ⚠️ PENTING - Security Note
- Kode di atas memberikan akses PUBLIC penuh
- **HANYA untuk development/testing**
- Untuk production, gunakan Firebase Authentication proper

---

## Initialize Database Structure

Emma database bisa bekerja auto-create, tetapi untuk structure yang rapi, inisialisasi manual:

### Option 1: Manual via Firebase Console

1. Di **Realtime Database**, klik lokasi data (usually `null` atau ada icon)
2. Klik **"+"** untuk add data
3. Buat struktur berikut:

```
Name: "devices"
  Value: (leave empty / click + to add children)
    
    Name: "esp32_01"
    Value: (empty)
      
      Name: "control"
      Value: (empty)
        - Name: "autoWatering" | Value: true
        - Name: "lcdMessage" | Value: ""
        - Name: "manualPump" | Value: false
      
      Name: "status"
      Value: (empty)
        - Name: "online" | Value: true
        - Name: "deviceName" | Value: "Smart Pump 01"
        - Name: "lastDuration" | Value: 0
        - Name: "lastUpdate" | Value: 0
        - Name: "lastWatered" | Value: ""
        - Name: "moisture" | Value: 0
        - Name: "pumpRunning" | Value: false
```

**Atau** copy-paste JSON ini:

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
        "lastDuration": 0,
        "lastUpdate": 0,
        "lastWatered": "",
        "moisture": 0,
        "online": true,
        "pumpRunning": false
      }
    }
  }
}
```

### Option 2: Auto-create by ESP32
- ESP32 akan auto-create fields saat pertama kali startup
- Tidak ada masalah, hanya struktur mungkin sedikit berbeda urutan

---

## Testing Data Flow

### Test 1: Verify ESP32 Upload Status

1. Upload code ESP32
2. Buka Serial Monitor
3. Tunggu sampai muncul status "Firebase Connected"
4. Di Firebase Console → Realtime Database → cek apakah `devices/esp32_01/status/online` menjadi `true`

Expected:
```json
{
  "devices": {
    "esp32_01": {
      "status": {
        "online": true,
        "moisture": (nilai sensor),
        "pumpRunning": false
      }
    }
  }
}
```

### Test 2: Manual Control via Firebase Console

1. Di Firebase Console, buka **Realtime Database**
2. Click pada `devices > esp32_01 > control > manualPump`
3. **Edit** value dari `false` → `true`
4. Seharusnya relay di ESP32 akan berbunyi "klik" dan pompa nyala
5. LCD harus menampilkan "PUMP: RUNNING"
6. Serial Monitor akan print `[PUMP] STARTED`

7. Ubah `manualPump` kembali ke `false`
8. Relay harus berbunyi lagi "klik" dan pompa mati
9. Serial print: `[PUMP] STOPPED - Duration: XX sec`

### Test 3: Monitor Sensor Data

1. Buka **Realtime Database** → scroll ke `status/moisture`
2. Seharusnya nilai terupdate setiap 2 detik (0-100%)
3. Coba basahkan sensor dengan air
4. Nilai seharusnya meningkat (karena capacitive sensor basah = lebih tinggi)

### Test 4: Check Connection Status

```
Expected Serial Output setiap ±5 detik:
[Firebase] Moisture:45% | Pump:OFF
```

Jika:
- ✅ Normal: Sensor baca dengan baik
- ❌ Moisture: 0% terus: Cek pin GPIO 34 & sensor wiring
- ❌ Pump tidak nyala: Cek GPIO 26 relay wiring

---

## Data Structure Reference

```
/ (root)
└── devices/
    └── esp32_01/                    Device ID
        ├── control/                 Input dari Android/manual
        │   ├── autoWatering: bool   (true/false)
        │   ├── lcdMessage: string   (text untuk LCD)
        │   └── manualPump: bool     (true untuk ON, false pour OFF)
        │
        └── status/                  Output dari ESP32
            ├── online: bool         Device aktif/offline
            ├── moisture: int        Kelembaban sensor (0-100%)
            ├── pumpRunning: bool    Pompa sedang running
            ├── lastWatered: string  Timestamp terakhir siram
            ├── lastDuration: int    Durasi siram terakhir (detik)
            ├── deviceName: string   Nama device untuk referensi
            └── lastUpdate: int      Unix timestamp update terakhir
```

---

## Integration dengan Android App

### From Android to ESP32

**Android APP → Firebase Database:**
```
Write to: devices/esp32_01/control/manualPump
Value: true (User tap "Siram Sekarang")
```

**ESP32 reads this every 5 seconds:**
```cpp
readFirebaseCommands();  // Di loop()
// Jika manualPump = true, nyalakan relay
```

### From ESP32 to Android

**ESP32 → Firebase Database:**
```
Write to: devices/esp32_01/status/
- moisture: 45
- pumpRunning: true
- lastWatered: "14:30:45"
```

**Android APP reads this:**
```
Read from: devices/esp32_01/status/
// Update UI dengan nilai terbaru
// Tampilkan gauge, status pompa, dll
```

---

## Monitoring Tips

### Via Firebase Console
1. **Real-time view:** Buka Database → semua perubahan visible instantly
2. **Check logs:** Jika ada error, cek di **Realtime Database → Activity**

### Via Serial Monitor (ESP32)
```
[SENSOR] Raw: 2500 | Moisture: 45%
[Firebase] Moisture:45% | Pump:OFF
[PUMP] STARTED
[PUMP] STOPPED
```

### Via Android Logcat (jika ada app logging)
```
Firebase: Data synced
Received pump status: ON
Updated UI with moisture: 45%
```

---

## Troubleshooting

### ❌ Database tidak muncul di Console
- Reload halaman Firebase Console
- Pastikan project sudah fully initialized
- Cek internet connection

### ❌ Credentials tidak working
- Double-check copy-paste (tidak ada spasi extra)
- Pastikan menggunakan **Database Secret**, bukan API Key
- Generate secret baru jika perlu

### ❌ Security Rules error
- Firebase akan warning jika rules tidak aman
- Publish ulang dengan kode di atas
- Untuk production: gunakan proper authentication

### ❌ Data tidak sync ke ESP32
- Cek WiFi connected
- Cek Database URL benar
- Lihat error di Serial Monitor
- Test manual via Firebase Console dulu

---

## Next Steps

1. ✅ Create Firebase Project
2. ✅ Setup Realtime Database
3. ✅ Get Credentials
4. ✅ Copy ke `smart_irrigation_esp32.ino`
5. ✅ Upload code ke ESP32
6. ✅ Verify data flow
7. ✅ Test manual controls
8. 📱 Integrate dengan Android app

---

**Selesai! Database Anda siap digunakan untuk Smart Irrigation System.** 🎉
