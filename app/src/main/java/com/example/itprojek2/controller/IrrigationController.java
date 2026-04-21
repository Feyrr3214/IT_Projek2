package com.example.itprojek2.controller;

import androidx.annotation.Nullable;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * IrrigationController — Pintu masuk utama untuk semua fitur kontrol irigasi.
 *
 * File ini adalah FACADE (pembungkus). Semua logika sudah dipecah ke file terpisah
 * dengan nama Bahasa Indonesia agar lebih mudah dicari:
 *
 *   ┌────────────────────────────────────────────────────────────────────┐
 *   │  File                    │ Tanggung Jawab                         │
 *   ├────────────────────────────────────────────────────────────────────┤
 *   │  KontrolPompa.java       │ Nyalakan/matikan pompa manual & otomatis│
 *   │  KontrolLcd.java         │ Kirim/hapus pesan ke layar LCD ESP32   │
 *   │  KontrolKelembaban.java  │ Simpan/baca batas kelembaban tanah     │
 *   │  KontrolJadwal.java      │ Simpan/baca jadwal penyiraman harian   │
 *   │  PendengarStatus.java    │ Listener status real-time dari ESP32   │
 *   │  StatusPerangkat.java    │ Model data status perangkat            │
 *   │  KallbackKontrol.java    │ Semua interface/callback               │
 *   └────────────────────────────────────────────────────────────────────┘
 *
 * Struktur Firebase yang digunakan:
 *   devices/{deviceId}/
 *     control/
 *       manualPump   : boolean
 *       autoWatering : boolean
 *       lcdMessage   : String ("baris1|baris2")
 *       threshold/   : { minMoisture, maxMoisture }
 *       schedule/    : { enabled, hour, minute, duration, time }
 *       newWifi/     : { ssid, pass }
 *     status/
 *       pumpRunning  : boolean
 *       moisture     : int (0-100%)
 *       lastWatered  : String
 *       lastDuration : int (detik)
 *       online       : boolean
 *       lastPing     : long
 */
public class IrrigationController {

    private final DatabaseReference refKontrol;
    private final DatabaseReference refPerangkat;

    // Sub-kontroler dengan nama Bahasa Indonesia
    private final KontrolPompa      kontrolPompa;
    private final KontrolLcd        kontrolLcd;
    private final KontrolKelembaban kontrolKelembaban;
    private final KontrolJadwal     kontrolJadwal;
    private final PendengarStatus   pendengarStatus;

    /**
     * Buat instance controller untuk perangkat tertentu.
     *
     * @param idPerangkat ID perangkat ESP32 di Firebase, contoh: "esp32_01"
     */
    public IrrigationController(String idPerangkat) {
        DatabaseReference refRoot = FirebaseDatabase.getInstance().getReference();
        refPerangkat = refRoot.child("devices").child(idPerangkat);
        refKontrol   = refPerangkat.child("control");

        // Inisialisasi semua sub-kontroler
        kontrolPompa      = new KontrolPompa(refKontrol);
        kontrolLcd        = new KontrolLcd(refKontrol);
        kontrolKelembaban = new KontrolKelembaban(refKontrol);
        kontrolJadwal     = new KontrolJadwal(refKontrol);
        pendengarStatus   = new PendengarStatus(refPerangkat);

        // Bersihkan pesan LCD lama saat app dibuka
        kontrolLcd.hapusPesan();

        // Inisialisasi default nilai Firebase jika node belum ada
        refKontrol.child("manualPump").addListenerForSingleValueEvent(
            new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        refKontrol.child("manualPump").setValue(false);
                        refKontrol.child("autoWatering").setValue(false);
                    }
                }
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            }
        );
    }

    // ═══════════════════════════════════════════════════
    //  POMPA  →  KontrolPompa.java
    // ═══════════════════════════════════════════════════

    /** Nyalakan pompa secara manual */
    public void startManualPump(@Nullable OnCommandListener listener) {
        kontrolPompa.nyalakanPompa(listener);
    }

    /** Matikan pompa yang sedang menyala */
    public void stopManualPump(@Nullable OnCommandListener listener) {
        kontrolPompa.matikanPompa(listener);
    }

    /** Aktifkan/nonaktifkan mode penyiraman otomatis berbasis kelembaban */
    public void setAutoWatering(boolean enabled, @Nullable OnCommandListener listener) {
        kontrolPompa.aturPenyiramanOtomatis(enabled, listener);
    }

    // ═══════════════════════════════════════════════════
    //  LCD  →  KontrolLcd.java
    // ═══════════════════════════════════════════════════

    /** Kirim pesan 2 baris ke LCD ESP32 */
    public void setLcdMessage(String line1, String line2) {
        kontrolLcd.kirimPesan(line1, line2);
    }

    /** Kirim pesan 1 baris ke LCD ESP32 */
    public void setLcdMessage(String line1) {
        kontrolLcd.kirimPesan(line1);
    }

    /** Hapus pesan LCD → ESP32 kembali ke tampilan status normal */
    public void clearLcdMessage() {
        kontrolLcd.hapusPesan();
    }

    // ═══════════════════════════════════════════════════
    //  BATAS KELEMBABAN  →  KontrolKelembaban.java
    // ═══════════════════════════════════════════════════

    /** Simpan batas kelembaban ke Firebase */
    public void saveMoistureThreshold(int minValue, int maxValue,
                                      @Nullable OnCommandListener listener) {
        kontrolKelembaban.simpanBatas(minValue, maxValue, listener);
    }

    /** Baca batas kelembaban dari Firebase */
    public void loadMoistureThreshold(OnThresholdLoadListener listener) {
        kontrolKelembaban.bacaBatas(listener::onLoaded);
    }

    // ═══════════════════════════════════════════════════
    //  JADWAL PENYIRAMAN  →  KontrolJadwal.java
    // ═══════════════════════════════════════════════════

    /** Simpan jadwal penyiraman ke Firebase */
    public void saveSchedule(int hour, int minute, int durationSec, boolean enabled,
                             @Nullable OnCommandListener listener) {
        kontrolJadwal.simpanJadwal(hour, minute, durationSec, enabled, listener);
    }

    /** Baca jadwal penyiraman dari Firebase */
    public void loadSchedule(OnScheduleLoadListener listener) {
        kontrolJadwal.bacaJadwal(listener::onLoaded);
    }

    // ═══════════════════════════════════════════════════
    //  WIFI  →  (langsung, sederhana)
    // ═══════════════════════════════════════════════════

    /** Kirim perintah ganti WiFi ESP32 ke Firebase */
    public void updateWifi(String ssid, String pass, @Nullable OnCommandListener listener) {
        java.util.Map<String, Object> wifiData = new java.util.HashMap<>();
        wifiData.put("ssid", ssid);
        wifiData.put("pass", pass);
        refKontrol.child("newWifi").setValue(wifiData)
                .addOnSuccessListener(unused -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e.getMessage()); });
    }

    // ═══════════════════════════════════════════════════
    //  STATUS REAL-TIME  →  PendengarStatus.java
    // ═══════════════════════════════════════════════════

    /** Mulai dengarkan status ESP32 secara real-time */
    public void listenToStatus(OnStatusUpdateListener listener) {
        pendengarStatus.mulai(listener);
    }

    /** Hentikan listener – panggil di onDestroyView() untuk mencegah memory leak */
    public void stopListening() {
        pendengarStatus.berhenti();
    }

    // ═══════════════════════════════════════════════════
    //  INTERFACE & DATA CLASS (Backward Compatibility)
    //  Fragment yang sudah ada TIDAK perlu diubah karena
    //  interface ini meneruskan ke KallbackKontrol.
    // ═══════════════════════════════════════════════════

    /** Callback umum untuk perintah ke Firebase */
    public interface OnCommandListener extends KallbackKontrol.PerintahListener {}

    /** Callback update status real-time ESP32 */
    public interface OnStatusUpdateListener extends KallbackKontrol.StatusListener {}

    /** Callback baca batas kelembaban dari Firebase */
    public interface OnThresholdLoadListener extends KallbackKontrol.BatasKelembabanListener {}

    /** Callback baca jadwal penyiraman dari Firebase */
    public interface OnScheduleLoadListener extends KallbackKontrol.JadwalListener {}

    /**
     * Data class status perangkat ESP32.
     * Meneruskan ke StatusPerangkat agar Fragment lama tidak perlu diubah.
     */
    public static class DeviceStatus extends StatusPerangkat {}
}
