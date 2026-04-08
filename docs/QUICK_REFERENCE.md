# ESP32 Smart Irrigation - Quick Reference Card

## ⚡ Konfigurasi Cepat (5 Menit)

### 1. WiFi & Firebase Credentials
Edit file `smart_irrigation_esp32.ino` di baris 58-61:

```cpp
#define WIFI_SSID         "SSID_WiFi_Anda"         // ← Ubah ke SSID WiFi
#define WIFI_PASSWORD     "Password_WiFi_Anda"    // ← Ubah ke password WiFi

#define FIREBASE_HOST     "project-xxxxx-rtdb.firebaseio.com"  // ← Dari Firebase
#define FIREBASE_AUTH     "SecretKeyDariFirebase"               // ← Dari Firebase
```

### 2. Dapatkan Firebase Credentials
📍 **Firebase Console → Project Settings → Service Accounts → Database Secrets**

Copas ke field di atas ↑

### 3. Pin Configuration (jika berbeda)
Edit baris 71-76 jika pin ESP32 Anda berbeda:

```cpp
#define RELAY_PIN         26    // GPIO untuk relay
#define SOIL_PIN          34    // GPIO ADC untuk sensor
#define LCD_SDA           21    // GPIO I2C SDA
#define LCD_SCL           22    // GPIO I2C SCL
```

### 4. Upload & Test
1. **Arduino IDE → Tools → Board → ESP32 Dev Module**
2. **Pilih COM port** yang benar
3. **Ctrl+U** untuk upload
4. Tekan tombol **BOOT** saat muncul "Connecting..."
5. Buka Serial Monitor (**Ctrl+Shift+M**)
6. Set baud rate **115200**

### 5. Cek Status di Serial Monitor
Harus muncul:
```
[WiFi] Connected! IP: 192.168.x.x
[Firebase] Connected & device set online!
[SETUP] Initialization complete!
```

---

## 🔧 Serial Commands untuk Testing

Ketik dalam Serial Monitor:
- **P** = Pump ON
- **S** = Pump STOP
- **M** = Show moisture level
- **T** = Toggle Auto/Manual mode
- **D** = Debug status lengkap
- **R** = Soft restart
- **H** = Help

---

## 📍 Wiring Reference (Default)

```
ESP32     │   Relay      │   Soil Sensor    │   LCD I2C
──────────┼──────────────┼──────────────────┼─────────────
GPIO 26   │   Signal (IN)│                  │
GPIO 34   │              │   A/ADC Pin      │
GPIO 21   │              │                  │   SDA
GPIO 22   │              │                  │   SCL
GND       │   GND        │   GND            │   GND
5V/3.3V   │   VCC        │   VCC            │   VCC
```

---

## 🔍 LCD I2C Address Finder

Jika LCD tidak tampil, gunakan kode ini untuk find address:

```cpp
#include <Wire.h>

void setup() {
  Serial.begin(115200);
  Wire.begin(21, 22);  // SDA=21, SCL=22
}

void loop() {
  for (byte i = 8; i < 120; i++) {
    Wire.beginTransmission(i);
    if (Wire.endTransmission() == 0) {
      Serial.print("Found: 0x");
      Serial.println(i, HEX);
    }
  }
  delay(3000);
}
```

Ganti `0x27` di kode main dengan address yang ditemukan.

---

## 📊 Sensor Moisture Threshold

```cpp
#define SOIL_THRESHOLD    30    // Mulai siram jika < 30%
#define SOIL_FULL         70    // Berhenti jika > 70%
#define MAX_PUMP_TIME     180   // Max 3 menit per cycle
```

---

## 🐛 Quick Troubleshooting

| Masalah | Solusi |
|---------|--------|
| WiFi tidak connect | Cek SSID/pass, pastikan 2.4 GHz |
| Firebase offline | Cek credentials, pastikan database sudah created |
| LCD blank | Cek I2C address, wiring GPIO 21/22 |
| Pump tidak nyala | Cek GPIO 26, relay wiring, supply 5V |
| Sensor 0%/100% terus | Calibrate: catat nilai kering & basah |
| Compiler error | Update Library Firebase & LiquidCrystal_I2C |

---

## 📲 Firebase Database Paths

Untuk testing manual via Firebase Console:

**Write** (Android/Manual):
```
devices/esp32_01/control/manualPump = true
devices/esp32_01/control/autoWatering = true
devices/esp32_01/control/lcdMessage = "Siram|Sedang Jalan"
```

**Read** (Status dari ESP32):
```
devices/esp32_01/status/moisture        // 0-100 %
devices/esp32_01/status/pumpRunning     // true/false
devices/esp32_01/status/online          // true/false
```

---

## ✅ Setup Checklist

- [ ] Arduino IDE installed
- [ ] ESP32 board pack installed
- [ ] Libraries installed (Firebase, LiquidCrystal_I2C)
- [ ] WiFi SSID & password dimasukkan
- [ ] Firebase credentials dimasukkan
- [ ] Code compiled tanpa error
- [ ] Wiring connected dengan benar
- [ ] Serial Monitor menunjukkan status OK
- [ ] Firebase database created & online
- [ ] Device muncul di Firebase dengan status "online"
- [ ] LCD menampilkan informasi
- [ ] Relay dapat ditest dengan serial command `P` & `S`

---

**Next Step:** Cek [ESP32_SETUP_GUIDE.md](ESP32_SETUP_GUIDE.md) untuk detail lengkap!
