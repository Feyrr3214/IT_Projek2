# Referensi Kode ESP32 Arduino — Smart Irrigation System

Kode ini untuk ESP32 yang membaca perintah dari Firebase Realtime Database,
mengontrol pompa (relay), dan menampilkan pesan di LCD 16x2.

> **Catatan**: Kode ini hanya referensi. Sesuaikan pin dan konfigurasi sesuai hardware ikam.

## Library yang Dibutuhkan (Install di Arduino IDE)
- `Firebase ESP32 Client` by Mobitz
- `LiquidCrystal_I2C`
- `WiFi` (bawaan ESP32)

## Wiring

| Komponen | Pin ESP32 |
|----------|-----------|
| LCD SDA  | GPIO 21   |
| LCD SCL  | GPIO 22   |
| Relay    | GPIO 26   |
| Soil Moisture Sensor | GPIO 34 (ADC) |

## Kode Arduino

```cpp
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <LiquidCrystal_I2C.h>

// ========================================
// KONFIGURASI — Ganti sesuai milik ikam
// ========================================
#define WIFI_SSID       "NAMA_WIFI_IKAM"
#define WIFI_PASSWORD   "PASSWORD_WIFI"

#define FIREBASE_HOST   "itprojek2-xxxxx-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH   "YOUR_DATABASE_SECRET_KEY"

#define DEVICE_ID       "esp32_01"

// Pin
#define RELAY_PIN       26
#define SOIL_PIN        34

// LCD 16x2 I2C (alamat biasanya 0x27 atau 0x3F)
LiquidCrystal_I2C lcd(0x27, 16, 2);

// Firebase
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

// Variabel
bool manualPump = false;
bool autoWatering = true;
String lcdMessage = "";
unsigned long lastStatusUpdate = 0;
unsigned long pumpStartTime = 0;
bool pumpRunning = false;

void setup() {
    Serial.begin(115200);

    // Setup pin
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW); // Pompa mati

    // Setup LCD
    lcd.init();
    lcd.backlight();
    lcd.setCursor(0, 0);
    lcd.print("Smart Irrigation");
    lcd.setCursor(0, 1);
    lcd.print("Memulai...");

    // Koneksi WiFi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Menghubungkan WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi Terhubung!");

    // Update LCD
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("WiFi Terhubung!");
    lcd.setCursor(0, 1);
    lcd.print(WiFi.localIP().toString().substring(0, 16));

    // Setup Firebase
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    // Set online
    String path = String("devices/") + DEVICE_ID + "/status/online";
    Firebase.setBool(firebaseData, path, true);

    delay(2000);
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Sistem Siap!");
    lcd.setCursor(0, 1);
    lcd.print("Menunggu...");
}

void loop() {
    // ========================================
    // 1. BACA PERINTAH DARI FIREBASE
    // ========================================

    String controlPath = String("devices/") + DEVICE_ID + "/control";

    // Baca manualPump
    if (Firebase.getBool(firebaseData, controlPath + "/manualPump")) {
        manualPump = firebaseData.boolData();
    }

    // Baca autoWatering
    if (Firebase.getBool(firebaseData, controlPath + "/autoWatering")) {
        autoWatering = firebaseData.boolData();
    }

    // Baca lcdMessage
    if (Firebase.getString(firebaseData, controlPath + "/lcdMessage")) {
        String newMsg = firebaseData.stringData();
        if (newMsg != lcdMessage && newMsg.length() > 0) {
            lcdMessage = newMsg;
            displayLcdMessage(lcdMessage);
        }
    }

    // ========================================
    // 2. BACA SENSOR KELEMBABAN
    // ========================================
    int sensorValue = analogRead(SOIL_PIN);
    // Konversi ke persentase (0-100%)
    // Sensor kapasitif: kering = nilai tinggi, basah = nilai rendah
    int moisture = map(sensorValue, 4095, 0, 0, 100);
    moisture = constrain(moisture, 0, 100);

    // ========================================
    // 3. LOGIKA KONTROL POMPA
    // ========================================
    bool shouldPumpRun = false;

    // Manual pump override
    if (manualPump) {
        shouldPumpRun = true;
    }
    // Auto watering: nyalakan jika kelembaban < 30%
    else if (autoWatering && moisture < 30) {
        shouldPumpRun = true;
    }

    // Kontrol relay
    if (shouldPumpRun && !pumpRunning) {
        digitalWrite(RELAY_PIN, HIGH);
        pumpRunning = true;
        pumpStartTime = millis();
        Serial.println("Pompa NYALA");
    } else if (!shouldPumpRun && pumpRunning) {
        digitalWrite(RELAY_PIN, LOW);
        pumpRunning = false;

        // Hitung durasi
        unsigned long duration = (millis() - pumpStartTime) / 1000;

        // Update status terakhir ke Firebase
        String statusPath = String("devices/") + DEVICE_ID + "/status";
        Firebase.setInt(firebaseData, statusPath + "/lastDuration", (int)duration);

        // Format waktu sederhana
        Firebase.setString(firebaseData, statusPath + "/lastWatered", getTimeString());

        Serial.println("Pompa MATI - Durasi: " + String(duration) + " detik");
    }

    // ========================================
    // 4. UPDATE STATUS KE FIREBASE (tiap 2 detik)
    // ========================================
    if (millis() - lastStatusUpdate > 2000) {
        lastStatusUpdate = millis();

        String statusPath = String("devices/") + DEVICE_ID + "/status";
        Firebase.setInt(firebaseData, statusPath + "/moisture", moisture);
        Firebase.setBool(firebaseData, statusPath + "/pumpRunning", pumpRunning);
        Firebase.setBool(firebaseData, statusPath + "/online", true);

        Serial.println("Kelembaban: " + String(moisture) + "% | Pompa: " + (pumpRunning ? "ON" : "OFF"));
    }

    delay(500);
}

// ========================================
// FUNGSI HELPER
// ========================================

/**
 * Tampilkan pesan di LCD 16x2.
 * Format pesan: "baris1|baris2"
 */
void displayLcdMessage(String message) {
    lcd.clear();

    int separatorIndex = message.indexOf('|');
    if (separatorIndex != -1) {
        String line1 = message.substring(0, separatorIndex);
        String line2 = message.substring(separatorIndex + 1);

        lcd.setCursor(0, 0);
        lcd.print(line1.substring(0, min((int)line1.length(), 16)));
        lcd.setCursor(0, 1);
        lcd.print(line2.substring(0, min((int)line2.length(), 16)));
    } else {
        lcd.setCursor(0, 0);
        lcd.print(message.substring(0, min((int)message.length(), 16)));
    }

    Serial.println("LCD: " + message);
}

/**
 * Dapatkan string waktu sederhana
 * (Untuk waktu akurat, gunakan NTP library)
 */
String getTimeString() {
    unsigned long seconds = millis() / 1000;
    unsigned long minutes = seconds / 60;
    unsigned long hours = minutes / 60;

    return String(hours % 24) + ":" +
           (minutes % 60 < 10 ? "0" : "") + String(minutes % 60) + ":" +
           (seconds % 60 < 10 ? "0" : "") + String(seconds % 60);
}
```

## Struktur Firebase yang Digunakan

```json
{
  "devices": {
    "esp32_01": {
      "control": {
        "manualPump": false,
        "autoWatering": true,
        "lcdMessage": "Pompa Manual|Sedang Aktif..."
      },
      "status": {
        "pumpRunning": false,
        "moisture": 45,
        "lastWatered": "08:45:00",
        "lastDuration": 30,
        "online": true
      }
    }
  }
}
```

## Alur Kerja

1. **User tekan "Siram Manual"** di aplikasi Android
2. Android menulis `manualPump = true` + `lcdMessage = "Pompa Manual|Sedang Aktif..."` ke Firebase
3. ESP32 membaca Firebase → nyalakan relay → tampilkan pesan di LCD
4. ESP32 menulis status `pumpRunning = true`, `moisture`, dll ke Firebase
5. Android membaca status → update UI (gauge, status pompa, dll)
6. **User tekan "Hentikan"** → `manualPump = false` → pompa mati → LCD update
