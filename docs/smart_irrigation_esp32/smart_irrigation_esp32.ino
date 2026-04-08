/*
 * ============================================================
 * SMART IRRIGATION SYSTEM - ESP32 MAIN CODE
 * ============================================================
 * Sistem Irigasi Otomatis dengan Firebase Realtime Database
 * 
 * Features:
 * - Membaca sensor kelembaban tanah (ADC)
 * - Kontrol pompa air via relay
 * - Display status di LCD 16x2 (I2C)
 * - Sinkronisasi data dengan Firebase Realtime Database
 * - Mode manual dan otomatis watering
 * - Monitoring online status
 * 
 * Hardware Required:
 * - ESP32 Development Board
 * - Relay Module / Solenoid
 * - Soil Moisture Sensor (Capacitive recommended)
 * - LCD 16x2 with I2C Module
 * - 5V Power Supply
 * 
 * Wiring:
 * | Komponen          | Pin ESP32 |
 * |-------------------|-----------|
 * | LCD SDA           | GPIO 21   |
 * | LCD SCL           | GPIO 22   |
 * | Relay Signal      | GPIO 26   |
 * | Soil Sensor ADC   | GPIO 34   |
 * | GND & 5V          | Common    |
 * 
 * Created: 2026
 * ============================================================
 */

#include <WiFi.h>
#include <FirebaseESP32.h>
#include <LiquidCrystal_I2C.h>
#include <time.h>
#include <BluetoothSerial.h>
#include <Preferences.h>

// ============================================================
// FUNCTION PROTOTYPES
// ============================================================
void initLCD();
void displayLCD(const char* line1, const char* line2);
void displayCustomLCDMessage(String message);
void updateLCDDisplay();
void loadWiFiCredentials();
void saveWiFiCredentials(String ssid, String pass);
void connectWiFi();
void handleWiFiDisconnection();
void startProvisioning();
void handleProvisioning();
void readSoilMoisture();
void setupFirebase();
void readFirebaseCommands();
void updateFirebaseStatus();
void executeControlLogic();
void startPump();
void stopPump();
String getFormattedTime();
void handleSerialInput();

// ============================================================
// KONFIGURASI WIFI & FIREBASE
// ============================================================
// Default credentials (sebagai fallback jika belum di-set via Bluetooth)
String wifi_ssid = "YOUR_WIFI_SSID";
String wifi_password = "YOUR_WIFI_PASSWORD";

#define FIREBASE_HOST     "it-projek-2-409eb-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH     "1qYZlTj70a3eE6dnzFsRNeSqnZJAp7y72iUFUOT9"

#define DEVICE_ID         "esp32_01"
#define DEVICE_NAME       "Smart Pump 01"

// ============================================================
// PIN CONFIGURATION
// ============================================================
#define RELAY_PIN         26    // Relay/Solenoid control pin
#define SOIL_PIN          32    // Soil moisture analog input (ADC)
#define LCD_SDA           21    // LCD I2C SDA
#define LCD_SCL           22    // LCD I2C SCL

// ============================================================
// SENSOR & CONTROL SETTINGS
// ============================================================
#define SOIL_THRESHOLD    30    // Mulai siram jika kelembaban < 30%
#define SOIL_FULL         70    // Berhenti siram jika kelembaban > 70%
#define MAX_PUMP_TIME     180   // Max pump duration: 3 minutes (detik)
#define MOISTURE_UPDATE_INTERVAL 2000  // Update status ke Firebase setiap 2 detik
#define LCD_UPDATE_INTERVAL      3000  // Update LCD setiap 3 detik
#define FIREBASE_RETRY_INTERVAL  5000  // Retry Firebase setiap 5 detik
#define NTP_SERVER               "pool.ntp.org"

// ============================================================
// LCD 16x2 SETUP (I2C Address: 0x27 atau 0x3F)
// ============================================================
LiquidCrystal_I2C lcd(0x27, 16, 2);

// ============================================================
// FIREBASE SETUP
// ============================================================
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

// ============================================================
// BLUETOOTH & PREFERENCES SETUP
// ============================================================
BluetoothSerial SerialBT;
Preferences preferences;
const char* BT_DEVICE_NAME = "ESP32_Irrigation_Setup";

// ============================================================
// GLOBAL VARIABLES - SYSTEM STATE
// ============================================================
struct SystemState {
  bool pumpRunning;
  bool manualPump;
  bool autoWatering;
  int soilMoisture;
  int sensorRaw;
  unsigned long pumpStartTime;
  unsigned long lastMoistureUpdate;
  unsigned long lastLcdUpdate;
  unsigned long lastFirebaseUpdate;
  String lastError;
  bool firebaseConnected;
  int failureCount;
  bool wifiConnected;
  bool provisioningMode;
};

SystemState state = {
  .pumpRunning = false,
  .manualPump = false,
  .autoWatering = true,
  .soilMoisture = 0,
  .sensorRaw = 0,
  .pumpStartTime = 0,
  .lastMoistureUpdate = 0,
  .lastLcdUpdate = 0,
  .lastFirebaseUpdate = 0,
  .lastError = "",
  .firebaseConnected = false,
  .failureCount = 0,
  .wifiConnected = false,
  .provisioningMode = false
};

// ============================================================
// SETUP FUNCTION
// ============================================================
void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n\n");
  Serial.println("========================================");
  Serial.println("SMART IRRIGATION SYSTEM - ESP32");
  Serial.println("Device ID: " + String(DEVICE_ID));
  Serial.println("========================================\n");

  // ---- Initialize Pins ----
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);
  Serial.println("[SETUP] Relay pin initialized (LOW)");

  // ---- Initialize LCD ----
  initLCD();
  displayLCD("Smart Irrig     ", "Booting...");

  // ---- Load WiFi from Flash ----
  loadWiFiCredentials();

  // ---- Connect WiFi ----
  connectWiFi();

  // Jika gagal konek setelah percobaan di connectWiFi(), masuk mode Bluetooth
  if (!state.wifiConnected) {
    startProvisioning();
  } else {
    // ---- normal setup if WiFi OK ----
    configTime(0, 0, NTP_SERVER);
    setupFirebase();
  }

  state.lastMoistureUpdate = millis();
  state.lastLcdUpdate = millis();
  state.lastFirebaseUpdate = millis();

  Serial.println("[SETUP] Initialization complete!\n");
}

// ============================================================
// MAIN LOOP
// ============================================================
void loop() {
  // 0. HANDLE PROVISIONING MODE (BLUETOOTH)
  if (state.provisioningMode) {
    handleProvisioning();
    return; // Stop main logic while in setup mode
  }

  handleSerialInput();
  
  if (WiFi.status() != WL_CONNECTED) {
    handleWiFiDisconnection();
    delay(1000);
    return;
  }

  unsigned long now = millis();

  readSoilMoisture();

  if (now - state.lastFirebaseUpdate >= FIREBASE_RETRY_INTERVAL) {
    readFirebaseCommands();
    state.lastFirebaseUpdate = now;
  }

  executeControlLogic();

  if (now - state.lastMoistureUpdate >= MOISTURE_UPDATE_INTERVAL) {
    updateFirebaseStatus();
    state.lastMoistureUpdate = now;
  }

  if (now - state.lastLcdUpdate >= LCD_UPDATE_INTERVAL) {
    updateLCDDisplay();
    state.lastLcdUpdate = now;
  }

  delay(100);
}

// ============================================================
// WIFI & PREFERENCES FUNCTIONS
// ============================================================
void loadWiFiCredentials() {
  preferences.begin("irrigation", false);
  String savedSsid = preferences.getString("ssid", "");
  String savedPass = preferences.getString("pass", "");
  preferences.end();

  if (savedSsid != "") {
    wifi_ssid = savedSsid;
    wifi_password = savedPass;
    Serial.println("[Flash] Wi-Fi loaded: " + wifi_ssid);
  } else {
    Serial.println("[Flash] No Wi-Fi credentials saved. Using defaults.");
  }
}

void saveWiFiCredentials(String ssid, String pass) {
  preferences.begin("irrigation", false);
  preferences.putString("ssid", ssid);
  preferences.putString("pass", pass);
  preferences.end();
  Serial.println("[Flash] Wi-Fi credentials saved to NVS.");
}

void connectWiFi() {
  Serial.println("[WiFi] Connecting to: " + wifi_ssid);
  displayLCD("WiFi Connecting ", "...");

  WiFi.mode(WIFI_STA);
  WiFi.begin(wifi_ssid.c_str(), wifi_password.c_str());

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WiFi] Connected!");
    state.wifiConnected = true;
    displayLCD("WiFi OK         ", WiFi.localIP().toString().c_str());
    delay(2000);
  } else {
    Serial.println("\n[WiFi] FAILED to connect!");
    displayLCD("WiFi Failed!    ", "Entering Setup");
    state.wifiConnected = false;
  }
}

void handleWiFiDisconnection() {
  Serial.println("[ERROR] WiFi disconnected!");
  if (state.pumpRunning) stopPump();
  
  displayLCD("No WiFi...      ", "Reconnecting");
  state.wifiConnected = false;
  state.firebaseConnected = false;
}

// ============================================================
// BLUETOOTH PROVISIONING FUNCTIONS
// ============================================================
void startProvisioning() {
  state.provisioningMode = true;
  SerialBT.begin(BT_DEVICE_NAME);
  Serial.println("[BT] Bluetooth Provisioning Started: " + String(BT_DEVICE_NAME));
  displayLCD("BLUETOOTH MODE  ", "Waiting for HP..");
}

void handleProvisioning() {
  if (SerialBT.available()) {
    String data = SerialBT.readStringUntil('\n');
    data.trim();
    
    Serial.println("[BT] Received: " + data);

    // Format: WIFI:SSID,PASS
    if (data.startsWith("WIFI:")) {
      int commaIndex = data.indexOf(',');
      if (commaIndex != -1) {
        String newSsid = data.substring(5, commaIndex);
        String newPass = data.substring(commaIndex + 1);
        
        displayLCD("Received WiFi!  ", newSsid.c_str());
        Serial.println("[BT] New SSID: " + newSsid);
        
        saveWiFiCredentials(newSsid, newPass);
        SerialBT.println("WiFi Terhubung"); // Feedback to Android App
        delay(1000);
        
        Serial.println("[SYSTEM] Restarting to apply new WiFi...");
        displayLCD("SAVED!          ", "Restarting...");
        delay(2000);
        ESP.restart();
      }
    }
  }

  // Blink LCD or show status every 2 seconds
  static unsigned long lastMsg = 0;
  if (millis() - lastMsg > 3000) {
    displayLCD("OPEN APP...     ", "Setup Device");
    lastMsg = millis();
  }
}

// ============================================================
// SENSOR FUNCTIONS
// ============================================================
void readSoilMoisture() {
  state.sensorRaw = analogRead(SOIL_PIN);
  state.soilMoisture = map(state.sensorRaw, 4095, 0, 0, 100);
  state.soilMoisture = constrain(state.soilMoisture, 0, 100);
}

// ============================================================
// FIREBASE FUNCTIONS
// ============================================================
void setupFirebase() {
  Serial.println("[Firebase] Initializing..");
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  delay(1000);
  
  String onlinePath = "devices/" + String(DEVICE_ID) + "/status/online";
  if (Firebase.setBool(firebaseData, onlinePath, true)) {
    Serial.println("[Firebase] Connected!");
    state.firebaseConnected = true;
  }
}

void readFirebaseCommands() {
  if (WiFi.status() != WL_CONNECTED) return;
  String controlPath = "devices/" + String(DEVICE_ID) + "/control";

  if (Firebase.getBool(firebaseData, controlPath + "/manualPump")) {
    state.manualPump = firebaseData.boolData();
  }
  if (Firebase.getBool(firebaseData, controlPath + "/autoWatering")) {
    state.autoWatering = firebaseData.boolData();
  }
  if (Firebase.getString(firebaseData, controlPath + "/lcdMessage")) {
    String newMsg = firebaseData.stringData();
    if (newMsg.length() > 0) displayCustomLCDMessage(newMsg);
  }
}

void updateFirebaseStatus() {
  if (WiFi.status() != WL_CONNECTED) return;
  String statusPath = "devices/" + String(DEVICE_ID) + "/status";
  
  Firebase.setInt(firebaseData, statusPath + "/moisture", state.soilMoisture);
  Firebase.setBool(firebaseData, statusPath + "/pumpRunning", state.pumpRunning);
  Firebase.setBool(firebaseData, statusPath + "/online", true);
  Firebase.setString(firebaseData, statusPath + "/deviceName", DEVICE_NAME);
}

// ============================================================
// PUMP CONTROL LOGIC
// ============================================================
void executeControlLogic() {
  unsigned long now = millis();
  bool shouldPumpRun = false;

  if (state.pumpRunning && (now - state.pumpStartTime > MAX_PUMP_TIME * 1000)) {
    stopPump();
    return;
  }

  if (state.manualPump) {
    shouldPumpRun = true;
  } else if (state.autoWatering) {
    if (state.soilMoisture < SOIL_THRESHOLD) shouldPumpRun = true;
    else if (state.soilMoisture > SOIL_FULL) shouldPumpRun = false;
    else shouldPumpRun = state.pumpRunning; // Maintain state if in between
  }

  if (shouldPumpRun && !state.pumpRunning) startPump();
  else if (!shouldPumpRun && state.pumpRunning) stopPump();
}

void startPump() {
  digitalWrite(RELAY_PIN, HIGH);
  state.pumpRunning = true;
  state.pumpStartTime = millis();
  Serial.println("[PUMP] STARTED");
}

void stopPump() {
  digitalWrite(RELAY_PIN, LOW);
  state.pumpRunning = false;
  unsigned long duration = (millis() - state.pumpStartTime) / 1000;
  
  String statusPath = "devices/" + String(DEVICE_ID) + "/status";
  Firebase.setInt(firebaseData, statusPath + "/lastDuration", (int)duration);
  Firebase.setString(firebaseData, statusPath + "/lastWatered", getFormattedTime());
}

// ============================================================
// LCD DISPLAY FUNCTIONS
// ============================================================
void initLCD() {
  lcd.init();
  lcd.backlight();
}

void displayLCD(const char* line1, const char* line2) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(line1);
  lcd.setCursor(0, 1);
  lcd.print(line2);
}

void displayCustomLCDMessage(String message) {
  lcd.clear();
  int separatorIndex = message.indexOf('|');
  if (separatorIndex != -1) {
    lcd.setCursor(0, 0);
    lcd.print(message.substring(0, separatorIndex).substring(0, 16));
    lcd.setCursor(0, 1);
    lcd.print(message.substring(separatorIndex + 1).substring(0, 16));
  } else {
    lcd.setCursor(0, 0);
    lcd.print(message.substring(0, 16));
  }
}

void updateLCDDisplay() {
  String line1 = state.pumpRunning ? "PUMP ON         " : (state.autoWatering ? "AUTO MODE       " : "STANDBY         ");
  String line2 = "M:" + String(state.soilMoisture) + "%";
  if (!state.firebaseConnected) line1 = "No Firebase     ";
  displayLCD(line1.c_str(), line2.c_str());
}

// ============================================================
// UTILITY FUNCTIONS
// ============================================================
String getFormattedTime() {
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  char buffer[20];
  strftime(buffer, sizeof(buffer), "%H:%M:%S", timeinfo);
  return String(buffer);
}

void handleSerialInput() {
  if (Serial.available()) {
    char cmd = Serial.read();
    if (cmd == 'R') ESP.restart();
    if (cmd == 'F') { // Force provisioning
      saveWiFiCredentials("", "");
      ESP.restart();
    }
  }
}

